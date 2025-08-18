package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.entity.client.EnrollmentInfo;
import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.Participant;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.*;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.enrollment.AssociateDto;
import com.adp.benefits.carrier.model.enrollment.MessageDto;
import com.adp.benefits.carrier.model.enrollment.PsoDataDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentInfoPerTenantService {

    private final EnrollmentInfoRepository enrollmentInfoRepository;
    private final ProcessStepOutcomeRepository processStepOutcomeRepository;
    private final ParticipantRepository participantRepository;
    private final PlanTypeEnrollInfoAssocRepository planTypeEnrollInfoAssocRepository;
    private final MessagesRepository messagesRepository;

    @Value("${cpm.apis.associateInfo.url}")
    private String associateInfoApi;

    private static final String DUMMY = "__DUMMY__";
    private static final ZoneId ET_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter OCCURRED_ON_FMT =
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm a 'ET'", Locale.US);

    @Transactional(
            transactionManager = "clientTransactionManager",
            propagation = Propagation.REQUIRES_NEW,
            readOnly = true)
    public EnrollmentInfoService.TenantChunk seekTenantPage(
            String tenantId,
            Instant windowStartInstant,
            Instant windowEndInstantExclusive,
            EnrollmentInfoService.TenantCursor priorCursor,
            int limit,
            Map<String, String> clientKeyToNameMap,
            Collection<String> clientIdsFilter,
            String carrierIdFilter,
            List<String> systemTypes,
            String state,
            String status,
            String associateSearch,
            String globalSearch,
            Collection<String> globalClientKeys) {
        log.info(
                "[{}] Seek page: from={}, to={}, priorCursor={}, limit={}",
                tenantId,
                windowStartInstant,
                windowEndInstantExclusive,
                priorCursor,
                limit);

        Filters filters =
                buildFilters(
                        carrierIdFilter,
                        state,
                        status,
                        associateSearch,
                        globalSearch,
                        systemTypes,
                        clientIdsFilter,
                        globalClientKeys);

        Dropdowns dropdowns =
                queryDropdowns(windowStartInstant, windowEndInstantExclusive, filters);

        List<ProcessStepOutcome> processStepOutcomes =
                queryPage(
                        windowStartInstant, windowEndInstantExclusive, priorCursor, limit, filters);

        if (processStepOutcomes.isEmpty()) {
            return new EnrollmentInfoService.TenantChunk(
                    tenantId, List.of(), priorCursor, dropdowns.carriers(), dropdowns.clientIds());
        }

        PageContext pageContext = loadPageContext(processStepOutcomes);

        List<PsoDataDto> rows = mapRows(processStepOutcomes, pageContext);

        ProcessStepOutcome lastOutcome = processStepOutcomes.getLast();
        EnrollmentInfoService.TenantCursor newCursor =
                new EnrollmentInfoService.TenantCursor(
                        toInstant(lastOutcome.getCreateDt()),
                        lastOutcome.getProcessStepOutcomeId());

        return new EnrollmentInfoService.TenantChunk(
                tenantId, rows, newCursor, dropdowns.carriers(), dropdowns.clientIds());
    }

    private record Filters(
            boolean hasClientIds,
            boolean hasCarrierId,
            boolean hasSystemTypes,
            boolean hasGlobalClientKeys,
            String carrierTerm,
            String stateTerm,
            String uiStatusTerm,
            String associateTerm,
            String globalTerm,
            Collection<String> clientIdsParam,
            Collection<String> systemTypesParam,
            Collection<String> globalClientKeysParam) {}

    private Filters buildFilters(
            String carrierIdFilter,
            String state,
            String status,
            String associateSearch,
            String globalSearch,
            List<String> systemTypes,
            Collection<String> clientIdsFilter,
            Collection<String> globalClientKeys) {

        boolean hasSystemTypes = systemTypes != null && !systemTypes.isEmpty();
        boolean hasClientIds = clientIdsFilter != null && !clientIdsFilter.isEmpty();
        boolean hasGlobalKeys = globalClientKeys != null && !globalClientKeys.isEmpty();

        String carrierTerm = carrierIdFilter == null ? "" : carrierIdFilter.trim();
        String stateTerm = state == null ? "" : state.trim();
        String uiStatus = status == null ? "" : status.trim();
        String associateTerm = normalizeOrEmpty(associateSearch);
        String globalTerm = normalizeOrEmpty(globalSearch);

        return new Filters(
                hasClientIds,
                !carrierTerm.isBlank(),
                hasSystemTypes,
                hasGlobalKeys,
                carrierTerm,
                stateTerm,
                uiStatus,
                associateTerm,
                globalTerm,
                hasClientIds ? clientIdsFilter : List.of(DUMMY),
                hasSystemTypes ? systemTypes : List.of(DUMMY),
                hasGlobalKeys ? globalClientKeys : List.of(DUMMY));
    }

    private Dropdowns queryDropdowns(
            Instant windowStartInstant, Instant windowEndInstantExclusive, Filters filters) {
        Set<String> clientIdsForDropdown =
                processStepOutcomeRepository.distinctClientIdsForDropdown(
                        windowStartInstant,
                        windowEndInstantExclusive,
                        filters.hasCarrierId(),
                        filters.carrierTerm(),
                        filters.stateTerm(),
                        filters.uiStatusTerm(),
                        filters.hasSystemTypes(),
                        filters.systemTypesParam(),
                        filters.associateTerm(),
                        filters.globalTerm(),
                        filters.hasGlobalClientKeys(),
                        filters.globalClientKeysParam());

        Set<String> carriersForDropdown =
                processStepOutcomeRepository.distinctCarriersForDropdown(
                        windowStartInstant,
                        windowEndInstantExclusive,
                        filters.hasClientIds(),
                        filters.clientIdsParam(),
                        filters.stateTerm(),
                        filters.uiStatusTerm(),
                        filters.hasSystemTypes(),
                        filters.systemTypesParam(),
                        filters.associateTerm(),
                        filters.globalTerm(),
                        filters.hasGlobalClientKeys(),
                        filters.globalClientKeysParam());

        return new Dropdowns(carriersForDropdown, clientIdsForDropdown);
    }

    private List<ProcessStepOutcome> queryPage(
            Instant windowStartInstant,
            Instant windowEndInstantExclusive,
            EnrollmentInfoService.TenantCursor priorCursor,
            int limit,
            Filters filters) {

        boolean isFirstPage =
                (priorCursor == null
                        || priorCursor.lastCreatedOn() == null
                        || priorCursor.lastPsoId() == null);

        return isFirstPage
                ? processStepOutcomeRepository.findFirstPageInWindowFiltered(
                windowStartInstant,
                windowEndInstantExclusive,
                filters.hasClientIds(),
                filters.clientIdsParam(),
                filters.hasCarrierId(),
                filters.carrierTerm(),
                filters.stateTerm(),
                filters.uiStatusTerm(),
                filters.hasSystemTypes(),
                filters.systemTypesParam(),
                filters.associateTerm(),
                filters.globalTerm(),
                filters.hasGlobalClientKeys(),
                filters.globalClientKeysParam(),
                limit)
                : processStepOutcomeRepository.findNextPageInWindowFiltered(
                windowStartInstant,
                windowEndInstantExclusive,
                filters.hasClientIds(),
                filters.clientIdsParam(),
                filters.hasCarrierId(),
                filters.carrierTerm(),
                filters.stateTerm(),
                filters.uiStatusTerm(),
                filters.hasSystemTypes(),
                filters.systemTypesParam(),
                filters.associateTerm(),
                filters.globalTerm(),
                filters.hasGlobalClientKeys(),
                filters.globalClientKeysParam(),
                priorCursor.lastCreatedOn(),
                priorCursor.lastPsoId(),
                limit);
    }

    private record PageContext(
            Map<String, String> psoIdToCarrier,
            Map<String, List<String>> psoIdToPlanNames,
            Map<String, Participant> associateMap,
            Map<String, List<Message>> psoIdToMessages) {}

    private PageContext loadPageContext(List<ProcessStepOutcome> processStepOutcomes) {
        Set<String> psoIds =
                processStepOutcomes.stream()
                        .map(ProcessStepOutcome::getProcessStepOutcomeId)
                        .collect(Collectors.toSet());

        Set<String> associateOids =
                processStepOutcomes.stream()
                        .map(ProcessStepOutcome::getAoid)
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toSet());

        Map<String, String> psoIdToCarrier =
                enrollmentInfoRepository.findByProcessStepOutcomeIds(psoIds).stream()
                        .collect(
                                Collectors.toMap(
                                        ei -> ei.getProcessStepOutcome().getProcessStepOutcomeId(),
                                        EnrollmentInfo::getCarrierId,
                                        (a, b) -> a));

        Map<String, List<String>> psoIdToPlanNames = loadPlanNamesByPsoIds(psoIds);

        Map<String, Participant> associateMap =
                associateOids.isEmpty()
                        ? Map.of()
                        : participantRepository.findByAoidIn(associateOids).stream()
                        .collect(
                                Collectors.toMap(
                                        Participant::getAoid, p -> p, (a, b) -> a));

        Map<String, List<Message>> psoIdToMessages =
                psoIds.isEmpty()
                        ? Map.of()
                        : messagesRepository.findByProcessStepOutcomeIdIn(psoIds).stream()
                        .collect(Collectors.groupingBy(Message::getProcessStepOutcomeId));

        return new PageContext(
                psoIdToCarrier,
                psoIdToPlanNames,
                associateMap,
                psoIdToMessages);
    }

    private List<PsoDataDto> mapRows(
            List<ProcessStepOutcome> processStepOutcomes,
            PageContext pageContext) {
        List<PsoDataDto> rows = new ArrayList<>(processStepOutcomes.size());

        for (ProcessStepOutcome outcome : processStepOutcomes) {
            String psoId = outcome.getProcessStepOutcomeId();

            String systemType = outcome.getSystemType();
            String clientName = outcome.getClientName();
            String carrierName = pageContext.psoIdToCarrier().get(psoId);
            List<String> planTypeNames =
                    pageContext.psoIdToPlanNames().getOrDefault(psoId, List.of());

            AssociateDto associateDto =
                    buildAssociateDto(outcome.getAoid(), pageContext.associateMap());
            List<MessageDto> messageDtos =
                    buildMessagesForPsoState(
                            outcome.getState(),
                            pageContext.psoIdToMessages().getOrDefault(psoId, List.of()));

            String backendStatus = outcome.getStatus() != null ? outcome.getStatus().name() : null;
            String backendState = outcome.getState() != null ? outcome.getState().name() : null;

            rows.add(
                    PsoDataDto.builder()
                            .systemType(systemType)
                            .clientName(clientName)
                            .carrierName(carrierName)
                            .transmissionId(outcome.getTransmissionId())
                            .correlationId(outcome.getCorrelationId())
                            .occurredOn(formatOccurredOnEt(outcome.getCreateDt()))
                            .occurredOnEpochMs(outcome.getCreateDt())
                            .status(mapUiStatus(backendStatus, backendState))
                            .state(backendState)
                            .associateDto(associateDto)
                            .planTypes(planTypeNames)
                            .messages(messageDtos.isEmpty() ? null : messageDtos)
                            .build());
        }
        return rows;
    }

    private Map<String, List<String>> loadPlanNamesByPsoIds(Set<String> psoIds) {
        if (psoIds == null || psoIds.isEmpty()) return Map.of();
        Map<String, List<String>> planNamesMap = new HashMap<>();
        for (Object[] row : planTypeEnrollInfoAssocRepository.findPlanNamesByPsoIds(psoIds)) {
            String psoId = (String) row[0];
            String plan = (String) row[1];
            planNamesMap.computeIfAbsent(psoId, __ -> new ArrayList<>()).add(plan);
        }
        return planNamesMap;
    }

    private AssociateDto buildAssociateDto(
            String associateOid, Map<String, Participant> associateMap) {
        if (associateOid == null || associateOid.isBlank()) return null;
        Participant participant = associateMap.get(associateOid);
        if (participant == null) return null;
        String fullName = buildFullName(participant.getFirstName(), participant.getLastName());
        String associateDetailsLink =
                String.format("%s?associateId=%s", associateInfoApi, participant.getAoid());
        return AssociateDto.builder()
                .fullName(fullName)
                .associateId(participant.getAoid())
                .associateDetailsLink(associateDetailsLink)
                .build();
    }

    private static String buildFullName(String firstName, String lastName) {
        boolean hasFirst = firstName != null && !firstName.isBlank();
        boolean hasLast = lastName != null && !lastName.isBlank();
        if (hasFirst && hasLast) return lastName + ", " + firstName;
        if (hasLast) return lastName;
        return hasFirst ? firstName : null;
    }

    private List<MessageDto> buildMessagesForPsoState(State state, List<Message> messageEntities) {
        if (messageEntities == null || messageEntities.isEmpty()) return List.of();

        return switch (state) {
            case VALIDATION_ERROR ->
                    parseAllMessages(messageEntities, this::parseValidationMessageNode);
            case CRITICAL_ERROR ->
                    parseAllMessages(messageEntities, this::parseCriticalErrorMessageNode);
            default -> parseAllMessages(messageEntities, this::parseResourceMessagesNode);
        };
    }

    private interface MessageNodeParser {
        List<MessageDto> parse(JsonNode rootNode);
    }

    private List<MessageDto> parseAllMessages(
            List<Message> messageEntities, MessageNodeParser parser) {
        List<MessageDto> result = new ArrayList<>();
        for (Message message : messageEntities) {
            JsonNode rootNode = message.getMessage();
            if (rootNode == null || rootNode.isNull()) continue;
            result.addAll(parser.parse(rootNode));
        }
        return result;
    }

    private List<MessageDto> parseValidationMessageNode(JsonNode rootNode) {
        List<Map<String, Object>> errorDetails =
                parseDynamicErrorDetails(rootNode.get("errorDetails"));
        String messageText = trimToNull(asText(rootNode.get("message")));
        if (messageText == null && errorDetails.isEmpty()) return List.of();
        return List.of(
                MessageDto.builder()
                        .message(messageText)
                        .errorDetails(errorDetails.isEmpty() ? null : errorDetails)
                        .build());
    }

    private List<MessageDto> parseCriticalErrorMessageNode(JsonNode rootNode) {
        List<Map<String, Object>> errorDetails =
                parseDynamicErrorDetails(rootNode.get("errorDetails"));
        String messageText = trimToNull(asText(rootNode.get("message")));
        if (messageText == null && errorDetails.isEmpty()) {
            messageText = trimToNull(asText(rootNode.get("errorDetails")));
        }
        if (messageText == null && errorDetails.isEmpty()) return List.of();
        return List.of(
                MessageDto.builder()
                        .message(messageText)
                        .errorDetails(errorDetails.isEmpty() ? null : errorDetails)
                        .build());
    }

    private List<MessageDto> parseResourceMessagesNode(JsonNode rootNode) {
        List<MessageDto> result = new ArrayList<>();
        JsonNode resourceMessages = rootNode.get("resourceMessages");
        if (resourceMessages == null || !resourceMessages.isArray()) return result;

        resourceMessages.forEach(
                resourceNode -> {
                    JsonNode messagesArray = resourceNode.get("messages");
                    if (messagesArray == null || !messagesArray.isArray()) return;
                    messagesArray.forEach(
                            messageNode -> {
                                String text = trimToNull(asText(messageNode.get("text")));
                                if (text != null)
                                    result.add(MessageDto.builder().message(text).build());
                            });
                });
        return result;
    }

    private List<Map<String, Object>> parseDynamicErrorDetails(JsonNode errorDetailsNode) {
        if (errorDetailsNode == null || errorDetailsNode.isNull()) return List.of();

        if (errorDetailsNode.isArray()) {
            return parseErrorDetailsArray(errorDetailsNode);
        }
        if (errorDetailsNode.isObject()) {
            Map<String, Object> asMap = toPlainMap(errorDetailsNode);
            return asMap.isEmpty() ? List.of() : List.of(asMap);
        }
        if (errorDetailsNode.isValueNode()) {
            return List.of(singletonValueMap(errorDetailsNode));
        }
        return List.of();
    }

    private List<Map<String, Object>> parseErrorDetailsArray(JsonNode arrayNode) {
        List<Map<String, Object>> detailsList = new ArrayList<>();
        arrayNode.forEach(
                itemNode -> {
                    if (itemNode == null || itemNode.isNull()) return;

                    if (itemNode.isObject()) {
                        Map<String, Object> mappedObject = toPlainMap(itemNode);
                        if (!mappedObject.isEmpty()) detailsList.add(mappedObject);
                    } else {
                        detailsList.add(singletonValueMap(itemNode));
                    }
                });
        return detailsList;
    }

    private Map<String, Object> singletonValueMap(JsonNode valueNode) {
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("value", toPlainJava(valueNode));
        return valueMap;
    }

    private Map<String, Object> toPlainMap(JsonNode objectNode) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        if (objectNode == null || objectNode.isNull() || !objectNode.isObject()) return resultMap;
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = objectNode.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldIterator.next();
            resultMap.put(entry.getKey(), toPlainJava(entry.getValue()));
        }
        return resultMap;
    }

    private Object toPlainJava(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isArray()) {
            List<Object> arrayValues = new ArrayList<>();
            node.forEach(child -> arrayValues.add(toPlainJava(child)));
            return arrayValues;
        }
        if (node.isObject()) return toPlainMap(node);
        return node.asText();
    }

    private static Instant toInstant(Object createdOnValue) {
        if (createdOnValue == null) return null;

        return switch (createdOnValue) {
            case Long longValue -> Instant.ofEpochMilli(longValue);
            case Number numberValue -> Instant.ofEpochMilli(numberValue.longValue());
            case Date dateValue -> dateValue.toInstant();
            case String stringValue -> {
                try {
                    yield Instant.ofEpochMilli(Long.parseLong(stringValue));
                } catch (NumberFormatException ignored) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private static String normalizeOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String asText(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText(null);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String formatOccurredOnEt(Object createdOnValue) {
        Instant instant = toInstant(createdOnValue);
        if (instant == null) return "-";
        ZonedDateTime easternTime = instant.atZone(ET_ZONE);
        return OCCURRED_ON_FMT.format(easternTime);
    }

    private static String mapUiStatus(String originalStatus, String state) {
        if (originalStatus == null) return null;

        return switch (originalStatus.toUpperCase()) {
            case "CONTINUE" -> "SUCCEEDED";
            case "ASYNC" ->
                    ("ONE_CG_ERROR".equalsIgnoreCase(state)
                            || "CARRIER_ERROR".equalsIgnoreCase(state))
                            ? "FAILED"
                            : "SUCCEEDED";
            case "STOP", "TIMEOUT", "INTERNAL_SERVER_ERROR" -> "FAILED";
            case "IN_PROGRESS" -> "IN_REVIEW";
            default -> originalStatus;
        };
    }

    private record Dropdowns(Set<String> carriers, Set<String> clientIds) {}
}