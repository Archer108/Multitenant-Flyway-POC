package com.adp.benefits.carrier.api.service.interfaces;

import com.adp.benefits.carrier.model.cpm.models.EvaluateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepResponse;
import reactor.core.publisher.Mono;

public interface IEvaluateStepService {
    /**
     * Reactive evaluation of a process step. May emit exactly one EvaluateStepResponse or error.
     */
    Mono<EvaluateStepResponse> evaluateStep(EvaluateStepRequest request);
}
