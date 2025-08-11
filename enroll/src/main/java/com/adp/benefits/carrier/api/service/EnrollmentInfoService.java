package com.adp.benefits.carrier.api.service;

import com.adp.benefits.carrier.config.database.TenantContextHolder;
import com.adp.benefits.carrier.config.database.TenantDatabaseLoader;
import com.adp.benefits.carrier.entity.primary.Client;
import com.adp.benefits.carrier.entity.primary.repository.ClientRepository;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoPageEnvelope;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoRequest;
import com.adp.benefits.carrier.model.enrollment.PsoDataDto;
import com.adp.benefits.carrier.model.enrollment.Timeframe;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Orchestrates multi-tenant, cursor-based pagination for Enrollment Info.
 * - Decodes incoming cursor into per-tenant seek positions
 * - Resolves dropdown selection (client name -> client_id key used in PSO)
 * - Derives client IDs that match global search (name contains)
 * - Calls the per-tenant worker for each tenant with all filters
 * - K-way merges rows, aggregates dependent dropdowns, returns nextCursor
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentInfoService {

    private final ClientRepository clientRepository;               // PRIMARY DB: client registry
    private final TenantDatabaseLoader tenantDatabaseLoader;       // Resolver: OID -> tenantId
    private final EnrollmentInfoPerTenantService perTenantService; // Per-tenant worker

    private static final int MERGE_EPSILON = 8; // small over-fetch per tenant to merge correctly
    private static final ObjectMapper MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Main cursor endpoint used by your reactive service impl.
     *
     * @param request filters and timeframe
     * @param cursor  opaque Base64 cursor returned by previous page (null/blank for first page)
     * @param size    desired page size (default 100 if null or < 1)
     */
    public EnrollmentInfoPageEnvelope getEnrollmentInfoCursor(
            EnrollmentInfoRequest request, String cursor, Integer size) {

        final int pageSize = (size == null || size < 1) ? 100 : size;

        // 1) Primary: client registry and maps
        final List<Client> primaryClients = clientRepository.findAll();

        // Map OID -> Name (used to render client names)
        final Map<String, String> oidToClientNameMap = new HashMap<>();
        // Map Name(lowercased) -> Set<OID> (handles possible duplicate names across tenants)
        final Map<String, Set<String>> nameToOidsMap = new HashMap<>();
        for (Client c : primaryClients) {
            final String oid = c.getOid();     // pso.client_id stores this OID
            final String name = c.getName();   // human readable
            if (oid == null || oid.isBlank()) continue;
            oidToClientNameMap.put(oid, name);
            if (name != null && !name.isBlank()) {
                nameToOidsMap.computeIfAbsent(name.trim().toLowerCase(), __ -> new HashSet<>()).add(oid);
            }
        }

        // 2) Tenant list (based on OID -> tenantId mapping)
        final List<String> tenantIds = primaryClients.stream()
                .map(c -> tenantDatabaseLoader.getTenantIdByOid(c.getOid()))
                .filter(Objects::nonNull)
                .toList();

        // 3) Time window
        final Instant[] window = resolveWindow(request);
        final Instant windowStart = window[0];
        final Instant windowEndExclusive = window[1];

        // 4) Decode cursor (or initialize a fresh one)
        final CursorState priorState =
                decodeOrInitializeCursor(cursor, request, pageSize, tenantIds, windowStart, windowEndExclusive);

        // 5) Resolve client filters
        //    - Dropdown selection (client name) -> set of client IDs (OIDs) to filter
        final Set<String> selectedClientKeys = resolveSelectedClientKeys(request.getClientId(), nameToOidsMap);
        //    - Global search: derive OIDs of clients whose names contain the global term
        final Set<String> globalClientKeys = deriveGlobalClientKeys(request.getGlobalSearch(), oidToClientNameMap);

        // 6) Fetch one chunk per tenant using seek + filters
        final Map<String, TenantCursor> newTenantCursors = new HashMap<>();
        final Set<String> carriersForDropdownUnion = new HashSet<>();
        final Set<String> clientIdsForDropdownUnion = new HashSet<>();
        final List<TenantChunk> tenantChunks = new ArrayList<>();

        for (String tenantId : tenantIds) {
            final TenantCursor priorCursor = priorState.getTenantCursors().get(tenantId);

            try {
                MDC.put("tenantId", tenantId);
                TenantContextHolder.setCurrentTenant(tenantId);

                final TenantChunk chunk = perTenantService.seekTenantPage(
                        tenantId,
                        windowStart,
                        windowEndExclusive,
                        priorCursor,
                        pageSize + MERGE_EPSILON,
                        oidToClientNameMap,                    // id/oid -> name
                        selectedClientKeys,                    // clientIds filter (from dropdown)
                        request.getCarrierId(),                  // carrierId filter (from dropdown; may be null)
                        request.getSystemTypes(),              // system types (mapped inside per-tenant)
                        request.getState() != null ? request.getState().name() : "",  // state
                        request.getStatus() != null ? request.getStatus() : "",       // status
                        request.getAssociateSearch(),          // associateSearch
                        request.getGlobalSearch(),             // globalSearch
                        globalClientKeys                       // derived from global search term
                );

                carriersForDropdownUnion.addAll(chunk.carriersForDropdown());
                clientIdsForDropdownUnion.addAll(chunk.clientIdsForDropdown());
                newTenantCursors.put(tenantId, chunk.lastCursor());
                tenantChunks.add(chunk);

            } finally {
                MDC.remove("tenantId");
                TenantContextHolder.clear();
            }
        }

        // 7) Merge across tenants and trim to page size
        final List<PsoDataDto> mergedRows = KWayMerger.merge(tenantChunks, pageSize);

        // 8) Convert union client IDs for dropdown to names
        final List<String> clientNamesForDropdown = clientIdsForDropdownUnion.stream()
                .map(id -> oidToClientNameMap.getOrDefault(id, id))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        // 9) Build next cursor state (reuse same filters/window; only cursors move forward)
        final CursorState nextState = priorState.withAdvancedCursors(newTenantCursors);
        final String nextCursor = encodeCursor(nextState);
        final boolean hasMore = mergedRows.size() == pageSize; // heuristic: if we filled the page, assume more

        return EnrollmentInfoPageEnvelope.builder()
                .clients(clientNamesForDropdown)                    // dependent dropdown (names)
                .carriers(new ArrayList<>(carriersForDropdownUnion))// dependent dropdown (ids/names as stored)
                .duration(request.getTimeframe())
                .psoData(mergedRows)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .pageSize(pageSize)
                .build();
    }

    // ---------- helpers: client key derivation ----------

    /**
     * Resolve dropdown "client" (human-readable name) to the underlying client_id keys (OIDs) used in PSO.
     * Returns empty set when no client selected (meaning "All").
     */
    private static Set<String> resolveSelectedClientKeys(String clientName, Map<String, Set<String>> nameToOidsMap) {
        if (clientName == null || clientName.isBlank()) return Set.of();
        final Set<String> oids = nameToOidsMap.get(clientName.trim().toLowerCase());
        return (oids == null) ? Set.of() : Set.copyOf(oids);
        // If you later decide to pass OID directly from UI, switch to: return Set.of(clientName);
    }

    /**
     * From global search term, derive the set of client_id keys (OIDs) whose display names contain the term.
     * Used to narrow data and dependent dropdowns when user types a client name in the global search box.
     */
    private static Set<String> deriveGlobalClientKeys(String term, Map<String, String> oidToClientNameMap) {
        if (term == null || term.isBlank()) return Set.of();
        final String needle = term.trim().toLowerCase();
        final Set<String> out = new HashSet<>();
        for (Map.Entry<String, String> e : oidToClientNameMap.entrySet()) {
            final String name = e.getValue();
            if (name != null && name.toLowerCase().contains(needle)) {
                out.add(e.getKey()); // add the OID (client_id stored in PSO)
            }
        }
        return out;
    }

    // ---------- Cursor helpers ----------

    private CursorState decodeOrInitializeCursor(String cursor,
                                                 EnrollmentInfoRequest request,
                                                 int pageSize,
                                                 List<String> tenantIds,
                                                 Instant windowStart,
                                                 Instant windowEndExclusive) {
        if (cursor == null || cursor.isBlank()) {
            // Fresh state: no per-tenant cursor yet
            Map<String, TenantCursor> blank = new HashMap<>();
            for (String t : tenantIds) blank.put(t, null);
            return new CursorState(
                    1,
                    request.getTimeframe() != null ? request.getTimeframe().name() : Timeframe.ONE_WEEK.name(),
                    windowStart.toEpochMilli(),
                    windowEndExclusive.toEpochMilli(),
                    pageSize,
                    tenantIds,
                    blank
            );
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            return MAPPER.readValue(json, new TypeReference<CursorState>() {});
        } catch (Exception ex) {
            log.warn("Failed to decode cursor, starting from first page. cause={}", ex.toString());
            return decodeOrInitializeCursor(null, request, pageSize, tenantIds, windowStart, windowEndExclusive);
        }
    }

    private String encodeCursor(CursorState state) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(state);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception ex) {
            log.warn("Failed to encode next cursor. cause={}", ex.toString());
            return null;
        }
    }

    private static Instant[] resolveWindow(EnrollmentInfoRequest request) {
        final Timeframe timeframe = request.getTimeframe() != null ? request.getTimeframe() : Timeframe.ONE_WEEK;
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (timeframe) {
            case TODAY -> new Instant[]{
                    today.atStartOfDay().toInstant(ZoneOffset.UTC),
                    today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
            case ONE_WEEK -> new Instant[]{
                    today.minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC),
                    today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
            case LAST_30_DAYS -> new Instant[]{
                    today.minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC),
                    today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
            case LAST_6_MONTHS -> new Instant[]{
                    today.minusMonths(6).atStartOfDay().toInstant(ZoneOffset.UTC),
                    today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
            case CUSTOM_DATE_RANGE -> {
                if (request.getDuration() != null
                        && request.getDuration().getStartDate() != null
                        && request.getDuration().getEndDate() != null) {
                    yield new Instant[]{
                            request.getDuration().getStartDate().atStartOfDay().toInstant(ZoneOffset.UTC),
                            request.getDuration().getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
                }
                // fallback to ONE_WEEK if custom range incomplete
                yield new Instant[]{
                        today.minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC),
                        today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)};
            }
        };
    }

    // ---------- Support types ----------

    /** One tenant’s seek position: last (createdOn, psoId) that was emitted. */
    public record TenantCursor(Instant lastCreatedOn, String lastPsoId) {}

    /**
     * One tenant’s page chunk: rows + the new cursor + dependent dropdown sets.
     * clientIdsForDropdown contains client_id keys (OIDs) which we convert to names in the orchestrator.
     */
    public record TenantChunk(
            String tenantId,
            List<PsoDataDto> rows,
            TenantCursor lastCursor,
            Set<String> carriersForDropdown,
            Set<String> clientIdsForDropdown
    ) {}

    /**
     * Cursor state we serialize as Base64 JSON between calls.
     * Includes window/timeframe/size/tenant list + per-tenant cursors.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CursorState {
        private int v; // schema version
        private String timeframe;
        private long windowStartEpochMs;
        private long windowEndEpochMs;
        private int size;
        private List<String> tenantIds;
        private Map<String, TenantCursor> tenantCursors;

        CursorState withAdvancedCursors(Map<String, TenantCursor> newCursors) {
            Map<String, TenantCursor> next = new HashMap<>(tenantCursors != null ? tenantCursors : Map.of());
            next.putAll(newCursors);
            return new CursorState(v, timeframe, windowStartEpochMs, windowEndEpochMs, size, tenantIds, next);
        }
    }

    /** Merge helper: takes the head of each tenant list and emits the top N by occurredOn desc (tie: transmissionId desc). */
    private static final class KWayMerger {
        static List<PsoDataDto> merge(List<TenantChunk> chunks, int pageSize) {
            PriorityQueue<Item> heap = new PriorityQueue<>((a, b) -> {
                long t1 = toEpoch(a.row.getOccurredOn());
                long t2 = toEpoch(b.row.getOccurredOn());
                if (t1 != t2) return Long.compare(t2, t1); // newer first
                String bId = safe(b.row.getTransmissionId());
                String aId = safe(a.row.getTransmissionId());
                return bId.compareTo(aId); // desc
            });

            for (TenantChunk c : chunks) {
                Iterator<PsoDataDto> it = c.rows.iterator();
                if (it.hasNext()) heap.add(new Item(c.tenantId, it.next(), it));
            }

            List<PsoDataDto> out = new ArrayList<>(pageSize);
            while (!heap.isEmpty() && out.size() < pageSize) {
                Item top = heap.poll();
                out.add(top.row);
                if (top.src.hasNext()) heap.add(new Item(top.tenantId, top.src.next(), top.src));
            }
            return out;
        }

        private static long toEpoch(Date d) { return d == null ? 0L : d.getTime(); }
        private static String safe(String s) { return s == null ? "" : s; }

        private record Item(String tenantId, PsoDataDto row, Iterator<PsoDataDto> src) {}
    }
}