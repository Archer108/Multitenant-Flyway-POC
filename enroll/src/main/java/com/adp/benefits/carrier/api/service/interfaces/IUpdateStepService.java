package com.adp.benefits.carrier.api.service.interfaces;

import com.adp.benefits.carrier.model.cpm.models.UpdateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepResponse;
import reactor.core.publisher.Mono;

public interface IUpdateStepService {
    Mono<UpdateStepResponse> updateStep(UpdateStepRequest request);
}
