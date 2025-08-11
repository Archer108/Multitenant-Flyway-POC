package com.adp.benefits.carrier.api.controller;

import com.adp.benefits.carrier.api.interfaces.ILogErrorStep;
import com.adp.benefits.carrier.api.service.interfaces.ILogErrorStepService;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepRequest;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class LogErrorStepController implements ILogErrorStep {

    private final ILogErrorStepService logErrorStepService;

    @Override
    public Mono<ResponseEntity<LogErrorStepResponse>> logStep(LogErrorStepRequest request) {
        return logErrorStepService.logError(request).map(ResponseEntity::ok);
    }
}
