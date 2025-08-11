package com.adp.benefits.carrier.api.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;

public interface EnrollmentInfoMapper {
    Set<String> extractCarrierIds(JsonNode payload);

    List<String> extractBenefitCoverages(JsonNode payload);
}
