package com.adp.benefits.carrier.api.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CanonicalEnrollmentInfoMapper implements EnrollmentInfoMapper {

    private static final String BENEFIT_COVERAGES_PATH = "benefitCoverages";
    private static final String COVERAGE_TYPE_CODE_PATH = "coverageTypeCode";
    private static final String CODE_PATH = "code";

    @Override
    public Set<String> extractCarrierIds(JsonNode payload) {
        if (payload == null) {
            return Collections.emptySet();
        }
        JsonNode carrierOrgNode = payload.path("carrierOrganization");
        if (carrierOrgNode.isMissingNode()) {
            return Collections.emptySet();
        }

        String carrierValue = carrierOrgNode.path("id").path("value").asText(null);
        return carrierValue != null ? Set.of(carrierValue) : Collections.emptySet();
    }

    @Override
    public List<String> extractBenefitCoverages(JsonNode payload) {
        List<String> coverageTypeCodes = new ArrayList<>();
        if (payload == null) {
            return coverageTypeCodes;
        }

        JsonNode coverages = payload.path(BENEFIT_COVERAGES_PATH);
        if (!coverages.isArray()) {
            return coverageTypeCodes;
        }

        for (JsonNode coverage : coverages) {
            JsonNode codeNode = coverage.path(COVERAGE_TYPE_CODE_PATH).path(CODE_PATH);
            if (codeNode.isTextual()) {
                coverageTypeCodes.add(codeNode.asText());
            }
        }
        return coverageTypeCodes;
    }
}
