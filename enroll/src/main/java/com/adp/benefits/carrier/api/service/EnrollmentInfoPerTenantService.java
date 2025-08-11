package com.adp.benefits.carrier.api.service;

import com.adp.benefits.carrier.entity.client.EnrollmentInfo;
import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.Participant;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.EnrollmentInfoRepository;
import com.adp.benefits.carrier.entity.client.repository.MessagesRepository;
import com.adp.benefits.carrier.entity.client.repository.ParticipantRepository;
import com.adp.benefits.carrier.entity.client.repository.PlanTypeEnrollInfoAssocRepository;
import com.adp.benefits.carrier.entity.client.repository.ProcessStepOutcomeRepository;
import com.adp.benefits.carrier.entity.primary.repository.ProcessStepRepository;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.enrollment.AssociateDto;
import com.adp.benefits.carrier.model.enrollment.ErrorDetailDto;
import com.adp.benefits.carrier.model.enrollment.MessageDto;
import com.adp.benefits.carrier.model.enrollment.PsoDataDto;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single-tenant data fetcher.
 * - Translates systemTypes -> processStepIds (PRIMARY DB).
 * - Computes filtered dropdown sets (clients & carriers) with proper dependency:
 *   * Clients dropdown ignores client filter, honors carrier (and others).
 *   * Carriers dropdown ignores carrier filter, honors client (and others).
 * - Performs seek pagination for the page.
 *
 * EXPECTED repository methods (native, not shown here):
 *   - ProcessStepRepository.findProcessStepIdsBySystemTypes(Collection<String>) : Set<String>
 *   - ProcessStepRepository.findSystemTypesByProcessStepIds(Set<String>) : List<Object[]> {process_step_id, system_type_tx}
 *   - ProcessStepOutcomeRepository.distinctClientIdsForDropdown(...) : Set<String>
 *   - ProcessStepOutcomeRepository.distinctCarriersForDropdown(...)  : Set<String>
 *   - ProcessStepOutcomeRepository.findFirstPageInWindowFiltered(...) / findNextPageInWindowFiltered(...)  (clientIds = Collection<String>, carrierId = String, + globalClientKeys support)
 *   - PlanTypeEnrollInfoAssocRepository.findPlanNamesByPsoIds(Set<String>) : List<Object[]>
 *   - EnrollmentInfoRepository.findByProcessStepOutcomeIds(Set<String>)    : List<EnrollmentInfo>
 *   - MessagesRepository.findByProcessStepOutcomeIdIn(Collection<String>)  : List<Message>
 *   - ParticipantRepository.findByAoidIn(Set<String>)                      : List<Participant>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentInfoPerTenantService {

    // CLIENT DB repos
    private final EnrollmentInfoRepository enrollmentInfoRepository;
    private final ProcessStepOutcomeRepository processStepOutcomeRepository;
    private final ParticipantRepository participantRepository;
    private final PlanTypeEnrollInfoAssocRepository planTypeEnrollInfoAssocRepository;
    private final MessagesRepository messagesRepository;

    // PRIMARY DB repo
    private final ProcessStepRepository processStepRepository;

    @Transactional(transactionManager = "clientTransactionManager",
            propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public EnrollmentInfoService.TenantChunk seekTenantPage(
            String tenantId,
            Instant windowStartInstant,
            Instant windowEndInstantExclusive,
            EnrollmentInfoService.TenantCursor priorCursor,
            int limit,
            Map<String, String> clientKeyToNameMap,
            Collection<String> clientIdsFilter,        // selected client keys (can be empty)
            String carrierIdFilter,                    // selected carrier (nullable/blank = all)
            List<String> systemTypes,                  // selected system types (names) -> mapped to process_step_ids
            String state,                              // exact match or blank
            String status,                             // exact match or blank
            String associateSearch,                    // AOID/first/last (ILIKE)
            String globalSearch,                       // global term (ILIKE) + client-name→keys
            Collection<String> globalClientKeys        // keys resolved from global client-name (can be empty)
    ) {
        log.info("[{}] Seek page: from={}, to={}, priorCursor={}, limit={}",
                tenantId, windowStartInstant, windowEndInstantExclusive, priorCursor, limit);

        // ---- Translate systemTypes -> process_step_ids (PRIMARY) ----
        final Set<String> processStepIds =
                (systemTypes == null || systemTypes.isEmpty())
                        ? Set.of()
                        : processStepRepository.findProcessStepIdsBySystemTypes(systemTypes);
        final boolean hasProcessSteps = !processStepIds.isEmpty();

        // ---- Normalize scalar terms ('' = not filtering) ----
        final String carrierTerm = carrierIdFilter == null ? "" : carrierIdFilter.trim();
        final String stateTerm   = state == null ? "" : state.trim();
        final String uiStatusTerm = status == null ? "" : status.trim();   // <-- UI value
        final String assocTerm   = assoc(associateSearch);
        final String globTerm    = assoc(globalSearch);

        // ---- Flags + safe params for IN() lists ----
        final boolean hasClientIds = clientIdsFilter != null && !clientIdsFilter.isEmpty();
        final Collection<String> clientIdsParam = hasClientIds ? clientIdsFilter : List.of("__DUMMY__");

        final boolean hasGlobalClientKeys = globalClientKeys != null && !globalClientKeys.isEmpty();
        final Collection<String> globalClientKeysParam =
                hasGlobalClientKeys ? globalClientKeys : List.of("__DUMMY__");

        final Collection<String> processStepIdsParam =
                hasProcessSteps ? processStepIds : List.of("__DUMMY__");

        final boolean hasCarrierId = !carrierTerm.isBlank();

        // ---- Dependent dropdowns ----
        // Clients dropdown: honor *all* filters EXCEPT the client selection (so UI can switch clients)
        final Set<String> clientIdsForDropdown =
                processStepOutcomeRepository.distinctClientIdsForDropdown(
                        windowStartInstant, windowEndInstantExclusive,
                        /* hasCarrierId */ hasCarrierId, carrierTerm,
                        stateTerm, uiStatusTerm,
                        hasProcessSteps, processStepIdsParam,
                        assocTerm, globTerm,
                        hasGlobalClientKeys, globalClientKeysParam
                );

        // Carriers dropdown: honor *all* filters including client selection (so carriers are scoped to client)
        final Set<String> carriersForDropdown =
                processStepOutcomeRepository.distinctCarriersForDropdown(
                        windowStartInstant, windowEndInstantExclusive,
                        /* hasClientIds */ hasClientIds, clientIdsParam,
                        stateTerm, uiStatusTerm,
                        hasProcessSteps, processStepIdsParam,
                        assocTerm, globTerm,
                        hasGlobalClientKeys, globalClientKeysParam
                );

        // ---- Page query (seek) ----
        final boolean firstPage = (priorCursor == null
                || priorCursor.lastCreatedOn() == null
                || priorCursor.lastPsoId() == null);

        final List<ProcessStepOutcome> processStepOutcomes = firstPage
                ? processStepOutcomeRepository.findFirstPageInWindowFiltered(
                windowStartInstant,
                windowEndInstantExclusive,
                /* clients */ hasClientIds, clientIdsParam,
                /* carrier */ hasCarrierId, carrierTerm,
                stateTerm, uiStatusTerm,
                hasProcessSteps, processStepIdsParam,
                assocTerm, globTerm,
                hasGlobalClientKeys, globalClientKeysParam,
                limit
        )
                : processStepOutcomeRepository.findNextPageInWindowFiltered(
                windowStartInstant,
                windowEndInstantExclusive,
                /* clients */ hasClientIds, clientIdsParam,
                /* carrier */ hasCarrierId, carrierTerm,
                stateTerm, uiStatusTerm,
                hasProcessSteps, processStepIdsParam,
                assocTerm, globTerm,
                hasGlobalClientKeys, globalClientKeysParam,
                priorCursor.lastCreatedOn(),
                priorCursor.lastPsoId(),
                limit
        );

        if (processStepOutcomes.isEmpty()) {
            return new EnrollmentInfoService.TenantChunk(
                    tenantId, List.of(), priorCursor, carriersForDropdown, clientIdsForDropdown);
        }

        // ---- Batch lookups for the page ----
        final java.util.Set<String> psoIds = processStepOutcomes.stream()
                .map(ProcessStepOutcome::getProcessStepOutcomeId)
                .collect(java.util.stream.Collectors.toSet());

        final java.util.Set<String> stepIds = processStepOutcomes.stream()
                .map(ProcessStepOutcome::getProcessStepId)
                .collect(java.util.stream.Collectors.toSet());

        final java.util.Set<String> associateOids = processStepOutcomes.stream()
                .map(ProcessStepOutcome::getAoid)
                .filter(java.util.Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        final java.util.Map<String, String> stepIdToSystemType =
                processStepRepository.findSystemTypesByProcessStepIds(stepIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                row -> (String) row[0],
                                row -> (String) row[1]
                        ));

        final java.util.Map<String, String> psoIdToCarrier =
                enrollmentInfoRepository.findByProcessStepOutcomeIds(psoIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                ei -> ei.getProcessStepOutcome().getProcessStepOutcomeId(),
                                com.adp.benefits.carrier.entity.client.EnrollmentInfo::getCarrierId,
                                (a, b) -> a
                        ));

        final java.util.Map<String, java.util.List<String>> psoIdToPlanNames = loadPlanNamesByPsoIds(psoIds);

        final java.util.Map<String, com.adp.benefits.carrier.entity.client.Participant> associateMap =
                associateOids.isEmpty()
                        ? java.util.Map.of()
                        : participantRepository.findByAoidIn(associateOids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.adp.benefits.carrier.entity.client.Participant::getAoid,
                                p -> p,
                                (a, b) -> a));

        final java.util.Map<String, java.util.List<com.adp.benefits.carrier.entity.client.Message>> psoIdToMessages =
                psoIds.isEmpty()
                        ? java.util.Map.of()
                        : messagesRepository.findByProcessStepOutcomeIdIn(psoIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                com.adp.benefits.carrier.entity.client.Message::getProcessStepOutcomeId));

        // ---- Map rows -> DTOs ----
        final java.util.List<PsoDataDto> rows = new java.util.ArrayList<>(processStepOutcomes.size());
        for (ProcessStepOutcome pso : processStepOutcomes) {
            final String psoId = pso.getProcessStepOutcomeId();
            final String systemType = stepIdToSystemType.get(pso.getProcessStepId());

            final String clientName = resolveClientName(clientKeyToNameMap, pso.getClientId(), pso.getClientId());
            final String carrierName = psoIdToCarrier.get(psoId);
            final java.util.List<String> planTypeNames = psoIdToPlanNames.getOrDefault(psoId, java.util.List.of());

            final AssociateDto associateDto = buildAssociateDto(pso.getAoid(), associateMap);
            final java.util.List<MessageDto> messageDtos =
                    buildMessagesForPsoState(pso.getState(), psoIdToMessages.getOrDefault(psoId, java.util.List.of()));

            final String backendStatus = pso.getStatus() != null ? pso.getStatus().name() : null;
            final String backendState  = pso.getState()  != null ? pso.getState().name()  : null;


            rows.add(PsoDataDto.builder()
                    .systemType(systemType)
                    .clientName(clientName)
                    .carrierName(carrierName)
                    .transmissionId(pso.getTransmissionId())
                    .correlationId(pso.getCorrelationId())
                    .occurredOn(toDate(pso.getCreateDt()))
                    .status(mapUiStatus(backendStatus, backendState))    // <-- UI status for response
                    .state(backendState)
                    .associateDto(associateDto)
                    .planTypes(planTypeNames)
                    .messages(messageDtos.isEmpty() ? null : messageDtos)
                    .build());
        }

        // ---- New cursor from last row ----
        final ProcessStepOutcome last = processStepOutcomes.get(processStepOutcomes.size() - 1);
        final EnrollmentInfoService.TenantCursor newCursor =
                new EnrollmentInfoService.TenantCursor(toInstant(last.getCreateDt()), last.getProcessStepOutcomeId());

        return new EnrollmentInfoService.TenantChunk(
                tenantId, rows, newCursor, carriersForDropdown, clientIdsForDropdown);
    }

    /** Maps backend status + state to the UI status used by the grid. */
    private static String mapUiStatus(String originalStatus, String state) {
        if (originalStatus == null) return null;
        switch (originalStatus.toUpperCase()) {
            case "CONTINUE":
                return "SUCCEEDED";
            case "ASYNC":
                if ("ONE_CG_ERROR".equalsIgnoreCase(state) || "CARRIER_ERROR".equalsIgnoreCase(state)) {
                    return "FAILED";
                }
                return "SUCCEEDED";
            case "IN_PROGRESS":
                return "IN_REVIEW";
            case "STOP":
            case "TIMEOUT":
            case "INTERNAL_SERVER_ERROR":
                return "FAILED";
            default:
                return originalStatus;
        }
    }

    /** Treat null as blank and trim. */
    private static String assoc(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------- dropdown helper queries result stitching ----------

    private Map<String, List<String>> loadPlanNamesByPsoIds(Set<String> psoIds) {
        if (psoIds == null || psoIds.isEmpty()) return Map.of();
        final Map<String, List<String>> out = new HashMap<>();
        for (Object[] row : planTypeEnrollInfoAssocRepository.findPlanNamesByPsoIds(psoIds)) {
            final String psoId = (String) row[0];
            final String plan  = (String) row[1];
            out.computeIfAbsent(psoId, __ -> new ArrayList<>()).add(plan);
        }
        return out;
    }

    private String resolveClientName(Map<String, String> clientKeyToNameMap,
                                     String psoClientId,
                                     String psoClientOid) {
        if (psoClientId != null && clientKeyToNameMap.containsKey(psoClientId)) {
            return clientKeyToNameMap.get(psoClientId);
        }
        if (psoClientOid != null && clientKeyToNameMap.containsKey(psoClientOid)) {
            return clientKeyToNameMap.get(psoClientOid);
        }
        return null;
    }

    // ---------- message mapping helpers ----------

    private static AssociateDto buildAssociateDto(String associateOid,
                                                  Map<String, Participant> associateMap) {
        if (associateOid == null || associateOid.isBlank()) return null;
        final Participant p = associateMap.get(associateOid);
        if (p == null) return null;
        final String fullName = buildFullName(p.getFirstName(), p.getLastName());
        return AssociateDto.builder()
                .fullName(fullName)
                .associateId(p.getAoid())
                .associateDetailsLink("apiLink")
                .build();
    }

    private static String buildFullName(String firstName, String lastName) {
        final boolean hasFirst = firstName != null && !firstName.isBlank();
        final boolean hasLast  = lastName  != null && !lastName.isBlank();
        if (hasFirst && hasLast) return lastName + ", " + firstName;
        if (hasLast) return lastName;
        return hasFirst ? firstName : null;
    }

    private List<MessageDto> buildMessagesForPsoState(State state, List<Message> messageEntities) {
        if (messageEntities == null || messageEntities.isEmpty()) return List.of();

        return switch (state) {
            case VALIDATION_ERROR -> parseAllMessages(messageEntities, this::parseValidationMessageNode);
            case CRITICAL_ERROR   -> parseAllMessages(messageEntities, this::parseCriticalErrorMessageNode);
            default               -> parseAllMessages(messageEntities, this::parseResourceMessagesNode);
        };
    }

    private interface MessageNodeParser { List<MessageDto> parse(JsonNode rootNode); }

    private List<MessageDto> parseAllMessages(List<Message> messageEntities, MessageNodeParser parser) {
        final List<MessageDto> result = new ArrayList<>();
        for (Message m : messageEntities) {
            final JsonNode root = m.getMessage();
            if (root == null || root.isNull()) continue;
            result.addAll(parser.parse(root));
        }
        return result;
    }

    private List<MessageDto> parseValidationMessageNode(JsonNode rootNode) {
        final List<ErrorDetailDto> details = extractValidationErrorDetails(rootNode.get("errorDetails"));
        final String msg = asText(rootNode.get("message"));
        if (isBlank(msg) && details.isEmpty()) return List.of();
        return List.of(MessageDto.builder()
                .message(isBlank(msg) ? null : msg.trim())
                .errorDetails(details.isEmpty() ? null : details)
                .build());
    }

    private List<ErrorDetailDto> extractValidationErrorDetails(JsonNode errorDetailsNode) {
        final List<ErrorDetailDto> details = new ArrayList<>();
        if (errorDetailsNode == null || !errorDetailsNode.isArray()) return details;
        errorDetailsNode.forEach(d -> {
            final String name = coalesce(asText(d.get("source")), asText(d.get("name")));
            final String value = extractFirstPrimitiveValue(d, Set.of("source", "name"));
            if (!isBlank(name) || !isBlank(value)) {
                details.add(ErrorDetailDto.builder()
                        .name(isBlank(name) ? null : name)
                        .value(isBlank(value) ? null : value)
                        .build());
            }
        });
        return details;
    }

    private List<MessageDto> parseCriticalErrorMessageNode(JsonNode rootNode) {
        final String msg = coalesce(asText(rootNode.get("errorDetails")), asText(rootNode.get("message")));
        if (isBlank(msg)) return List.of();
        return List.of(MessageDto.builder().message(msg.trim()).build());
    }

    private List<MessageDto> parseResourceMessagesNode(JsonNode rootNode) {
        final List<MessageDto> result = new ArrayList<>();
        final JsonNode rms = rootNode.get("resourceMessages");
        if (rms == null || !rms.isArray()) return result;

        rms.forEach(rm -> {
            final JsonNode msgs = rm.get("messages");
            if (msgs == null || !msgs.isArray()) return;
            msgs.forEach(m -> {
                final String text = asText(m.get("text"));
                if (!isBlank(text)) result.add(MessageDto.builder().message(text.trim()).build());
            });
        });
        return result;
    }

    // ---------- tiny utils ----------

//    private static String assoc(String s) { return s == null ? "" : s; }

    private static Instant toInstant(Object createdOnValue) {
        if (createdOnValue == null) return null;
        if (createdOnValue instanceof Long l) return Instant.ofEpochMilli(l);
        if (createdOnValue instanceof Number n) return Instant.ofEpochMilli(n.longValue());
        if (createdOnValue instanceof Date d) return d.toInstant();
        if (createdOnValue instanceof String s) {
            try { return Instant.ofEpochMilli(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Date toDate(Object createdOnValue) {
        if (createdOnValue == null) return null;
        if (createdOnValue instanceof Date d) return d;
        if (createdOnValue instanceof Long l) return new Date(l);
        if (createdOnValue instanceof Number n) return new Date(n.longValue());
        if (createdOnValue instanceof String s) {
            try { return new Date(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String extractFirstPrimitiveValue(JsonNode objectNode, Set<String> excludedKeys) {
        if (objectNode == null || !objectNode.isObject()) return null;
        final Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> e = it.next();
            final String k = e.getKey();
            if (excludedKeys != null && excludedKeys.contains(k)) continue;
            final JsonNode v = e.getValue();
            if (v != null && !v.isNull() && (v.isTextual() || v.isNumber() || v.isBoolean())) {
                return v.asText();
            }
        }
        return null;
    }

    private static String coalesce(String a, String b) { return !isBlank(a) ? a : b; }
    private static String asText(JsonNode node) { return (node == null || node.isNull()) ? null : node.asText(null); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}