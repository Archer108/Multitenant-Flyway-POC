package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.LogErrorStepService;
import com.adp.benefits.carrier.api.service.interfaces.ILogErrorStepService;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepRequest;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@Slf4j
public class LogErrorStepServiceImpl implements ILogErrorStepService {

    private final LogErrorStepService service;
    private final Scheduler workerScheduler;
    private final Scheduler timerScheduler;

    @Value("${cpm.api-timeouts.logError-step:30000ms}")
    private Duration timeout;

    public LogErrorStepServiceImpl(
            LogErrorStepService service,
            @Qualifier("workerScheduler") Scheduler workerScheduler,
            @Qualifier("timerScheduler") Scheduler timerScheduler) {
        this.service = service;
        this.workerScheduler = workerScheduler;
        this.timerScheduler = timerScheduler;
    }

    @Override
    public Mono<LogErrorStepResponse> logError(LogErrorStepRequest request) {
        log.error(
                "System: {}, Timestamp: {}",
                request.getSystemId(),
                request.getErrorInfo().getTimestamp());

        String tid =
                request.getTransmissionId() != null ? request.getTransmissionId().toString() : null;
        Instant start = Instant.now();

        return Mono.fromCallable(() -> service.appendErrorMessagesToProcessStepOutcome(request))
                .subscribeOn(workerScheduler)
                .timeout(timeout, timerScheduler)
                .doOnSuccess(
                        response -> {
                            Long elapsed = Duration.between(start, Instant.now()).toMillis();
                            log.info(
                                    "event=updateStep.completed timestamp={} tid={} elapsed={}ms",
                                    response.getTimestamp(),
                                    tid,
                                    elapsed);
                        })
                .onErrorResume(
                        ex -> {
                            log.error(
                                    "Failed to log error for transmission ID: {}, Reason: {}",
                                    tid,
                                    ex.getMessage(),
                                    ex);
                            return Mono.just(
                                    LogErrorStepResponse.builder().timestamp(new Date()).build());
                        });
    }
}
