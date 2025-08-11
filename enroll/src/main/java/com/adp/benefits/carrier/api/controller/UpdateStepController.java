package com.adp.benefits.carrier.api.controller;

import com.adp.benefits.carrier.api.interfaces.IUpdateStep;
import com.adp.benefits.carrier.api.service.interfaces.IUpdateStepService;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UpdateStepController implements IUpdateStep {

    private final IUpdateStepService updateStepService;

    @Override
    public Mono<UpdateStepResponse> updateStep(UpdateStepRequest request) {
        log.info(
                "event=updateStep.request tid={} and processStep={}, payload={}",
                request.getTransmissionId(),
                request.getProcessStepId(),
                request.getPayload());
        return updateStepService.updateStep(request);
    }
}
