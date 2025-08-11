package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.UpdateStepService;
import com.adp.benefits.carrier.api.service.interfaces.IUpdateStepService;
import com.adp.benefits.carrier.model.cpm.enums.AsyncStatus;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@Slf4j
public class UpdateStepServiceImpl implements IUpdateStepService {

    private final UpdateStepService service;
    private final Scheduler workerScheduler;
    private final Scheduler timerScheduler;

    @Value("${cpm.api-timeouts.update-step:30000ms}")
    private Duration timeout;

    public UpdateStepServiceImpl(
            UpdateStepService service,
            @Qualifier("workerScheduler") Scheduler workerScheduler,
            @Qualifier("timerScheduler") Scheduler timerScheduler) {
        this.service = service;
        this.workerScheduler = workerScheduler;
        this.timerScheduler = timerScheduler;
    }

    /**
     * This method is used to update the step in the system.
     *
     * @param request The request object containing the details of the step to be updated.
     * @return A Mono containing the response of the update operation.
     */
    @Override
    public Mono<UpdateStepResponse> updateStep(UpdateStepRequest request) {
        String tid = request.getTransmissionId().toString();
        Instant start = Instant.now();

        return Mono.fromCallable(() -> service.appendMessageToProcessStepOutcome(request))
                .subscribeOn(workerScheduler)
                .timeout(timeout, timerScheduler)
                .doOnSuccess(
                        response -> {
                            long elapsed = Duration.between(start, Instant.now()).toMillis();
                            log.info(
                                    "event=updateStep.completed cid={} elapsed={}ms",
                                    response.getCorrelationId(),
                                    tid,
                                    elapsed);
                        })
                .onErrorResume(
                        ex -> {
                            AsyncStatus status = getAsyncStatus(ex, start, tid);
                            return Mono.just(
                                    UpdateStepResponse.builder()
                                            .timestamp(new Date())
                                            .correlationId(null)
                                            .transmissionId(UUID.fromString(tid))
                                            .asyncStatus(status)
                                            .build());
                        });
    }

    private AsyncStatus getAsyncStatus(Throwable ex, Instant start, String tid) {
        long elapsed = Duration.between(start, Instant.now()).toMillis();
        AsyncStatus status = AsyncStatus.FAILED;

        if (ex instanceof java.util.concurrent.TimeoutException) {
            status = AsyncStatus.TIMEOUT;
            log.warn(
                    "event=updateStep.timeout tid={} elapsed={}ms timeout={}ms",
                    tid,
                    elapsed,
                    timeout.toMillis());
        } else {
            log.warn(
                    "event=updateStep.failed tid={} elapsed={}ms exception={}",
                    tid,
                    elapsed,
                    ex.toString());
        }

        return status;
    }
}
