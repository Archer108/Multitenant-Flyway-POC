package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.EvaluateStepService;
import com.adp.benefits.carrier.api.service.interfaces.IEvaluateStepService;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@Slf4j
public class EvaluateStepServiceImpl implements IEvaluateStepService {

    private final EvaluateStepService service;
    private final Scheduler workerScheduler;

    public EvaluateStepServiceImpl(
            EvaluateStepService service, @Qualifier("workerScheduler") Scheduler workerScheduler) {
        this.service = service;
        this.workerScheduler = workerScheduler;
    }

    @Override
    public Mono<EvaluateStepResponse> evaluateStep(EvaluateStepRequest request) {
        // everything—including blocking syncService.evaluate(...)—runs on virtual threads
        return Mono.fromCallable(() -> service.evaluate(request)).subscribeOn(workerScheduler);
    }
}
