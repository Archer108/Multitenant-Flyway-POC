package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.EnrollmentInfoService;
import com.adp.benefits.carrier.api.service.interfaces.IEnrollmentInfoService;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoPageEnvelope;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoRequest;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/** Executes blocking service on a worker Scheduler with a timeout. */
@Slf4j
@Service
public class EnrollmentInfoServiceImpl implements IEnrollmentInfoService {

    private final EnrollmentInfoService service;
    private final Scheduler workerScheduler;
    private final Scheduler timerScheduler;
    private final Duration timeout;

    public EnrollmentInfoServiceImpl(
            EnrollmentInfoService service,
            @Qualifier("workerScheduler") Scheduler workerScheduler,
            @Qualifier("timerScheduler") Scheduler timerScheduler,
            @Value("${cpm.api-timeouts.update-step:30000ms}") Duration timeout) {
        this.service = service;
        this.workerScheduler = workerScheduler;
        this.timerScheduler = timerScheduler;
        this.timeout = timeout;
    }

    @Override
    public Mono<EnrollmentInfoPageEnvelope> getEnrollmentInfoCursor(
            EnrollmentInfoRequest request, String cursor, Integer size) {

        return Mono.fromCallable(() -> service.getEnrollmentInfoCursor(request, cursor, size))
                .subscribeOn(workerScheduler)
                .timeout(timeout, timerScheduler);
    }
}
