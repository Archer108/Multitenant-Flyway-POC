package com.adp.benefits.carrier.api.service.interfaces;

import com.adp.benefits.carrier.model.cpm.models.LogErrorStepRequest;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepResponse;
import reactor.core.publisher.Mono;

public interface ILogErrorStepService {
    Mono<LogErrorStepResponse> logError(LogErrorStepRequest request);
}
