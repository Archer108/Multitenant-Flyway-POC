package com.adp.benefits.carrier.api.service.mapper;

import com.adp.benefits.carrier.entity.primary.ProcessStep;
import com.adp.benefits.carrier.entity.primary.SystemEntity;
import com.adp.benefits.carrier.entity.primary.repository.ProcessStepRepository;
import java.util.Optional;

public final class MapperUtil {

    // Private constructor to prevent instantiation
    private MapperUtil() {}

    public static SystemEntity mapProcessStepIdToSystemEntity(
            String processStepId, ProcessStepRepository processStepRepository) {
        Optional<ProcessStep> stepOpt = processStepRepository.findById(processStepId);
        if (stepOpt.isEmpty()) {
            throw new RuntimeException("ProcessStep not found for processStepId: " + processStepId);
        }
        ProcessStep step = stepOpt.get();
        return step.getProcessEntity().getSystemEntity();
    }
}
