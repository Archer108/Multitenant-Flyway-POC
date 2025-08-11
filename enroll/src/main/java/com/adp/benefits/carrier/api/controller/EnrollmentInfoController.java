package com.adp.benefits.carrier.api.controller;

import com.adp.benefits.carrier.api.interfaces.IEnrollmentInfo;
import com.adp.benefits.carrier.api.service.interfaces.IEnrollmentInfoService;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.enrollment.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EnrollmentInfoController implements IEnrollmentInfo {

    private final IEnrollmentInfoService enrollmentInfoService;

    @Override
    public Mono<ResponseEntity<EnrollmentInfoPageEnvelope>> getEnrollmentInfo(
            String timeframe,
            EnrollmentInfoRequest.Duration duration,
            String state,
            String status,
            List<String> systemTypes,     // <-- list
            String clientId,
            String carrierId,
            String associateSearch,
            String globalSearch,
            String cursor,
            Integer size) {

        Timeframe timeframeEnum = parseTimeframeOrDefault(timeframe, Timeframe.ONE_WEEK);
        EnrollmentInfoRequest.Duration appliedDuration =
                (timeframeEnum == Timeframe.CUSTOM_DATE_RANGE) ? duration : null;

        // Normalize list inputs (supports repeated params and single comma-separated param)
        List<String> normalizedSystemTypes = normalizeList(systemTypes);

        EnrollmentInfoRequest request = EnrollmentInfoRequest.builder()
                .timeframe(timeframeEnum)
                .duration(appliedDuration)
                .state(parseStateOrNull(state))          // tolerant: returns null if invalid
                .status(isBlank(status) ? null : status)
                .systemTypes(normalizedSystemTypes)      // pass list straight through
                .clientId(isBlank(clientId) ? null : clientId)
                .carrierId(isBlank(carrierId) ? null : carrierId)
                .associateSearch(isBlank(associateSearch) ? null : associateSearch)
                .globalSearch(isBlank(globalSearch) ? null : globalSearch)
                .build();

        return enrollmentInfoService
                .getEnrollmentInfoCursor(request, cursor, size)
                .map(ResponseEntity::ok);
    }

    /* ----- helpers (keep private inside the controller) ----- */

    private Timeframe parseTimeframeOrDefault(String raw, Timeframe fallback) {
        if (isBlank(raw)) return fallback;
        try { return Timeframe.valueOf(raw.trim()); }
        catch (IllegalArgumentException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Ignoring invalid timeframe='{}'; using {}", raw, fallback);
            return fallback;
        }
    }

    private State parseStateOrNull(String raw) {
        if (isBlank(raw)) return null;
        try { return State.valueOf(raw.trim()); }
        catch (IllegalArgumentException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Ignoring invalid state='{}' (use 'status' for UI status).", raw);
            return null;
        }
    }

    private List<String> normalizeList(List<String> in) {
        if (in == null || in.isEmpty()) return null;
        // Support both repeated params (?systemTypes=A&systemTypes=B) and comma-separated (?systemTypes=A,B)
        if (in.size() == 1 && in.get(0) != null && in.get(0).contains(",")) {
            String[] split = in.get(0).split(",");
            List<String> out = new java.util.ArrayList<>(split.length);
            for (String s : split) if (!isBlank(s)) out.add(s.trim());
            return out.isEmpty() ? null : out;
        }
        List<String> out = new java.util.ArrayList<>(in.size());
        for (String s : in) if (!isBlank(s)) out.add(s.trim());
        return out.isEmpty() ? null : out;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}