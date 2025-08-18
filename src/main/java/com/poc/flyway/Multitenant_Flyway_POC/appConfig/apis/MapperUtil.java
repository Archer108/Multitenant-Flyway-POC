package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.entity.primary.Client;
import com.adp.benefits.carrier.entity.primary.ProcessStep;
import com.adp.benefits.carrier.entity.primary.SystemEntity;
import com.adp.benefits.carrier.entity.primary.repository.ClientRepository;
import com.adp.benefits.carrier.entity.primary.repository.ProcessStepRepository;
import com.adp.benefits.carrier.exceptions.ProcessStepValidationException;
import jakarta.validation.constraints.NotNull;

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

    public static String resolveClientName(String clientOid, ClientRepository clientRepository) {
        return clientRepository.findByOid(clientOid)
                .map(Client::getName)
                .filter(n -> !n.isBlank())
                .orElseThrow(() ->
                        new ProcessStepValidationException("Client Name is required for client OID=" + clientOid));
    }

    public static @NotNull(message = "System Type is required") String resolveSystemType(String processStepId, ProcessStepRepository processStepRepository) {
        SystemEntity system = MapperUtil.mapProcessStepIdToSystemEntity(processStepId, processStepRepository);
        String systemType = (system != null) ? system.getSystemType() : null;
        if (systemType == null || systemType.isBlank()) {
            throw new ProcessStepValidationException("System Type is required for processStepId=" + processStepId);
        }
        return systemType;
    }
}
