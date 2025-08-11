package com.adp.benefits.carrier.api.service;

import com.adp.benefits.carrier.api.service.interfaces.IEvaluateStepService;
import com.adp.benefits.carrier.model.cpm.enums.AsyncStatus;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepResponse;
import com.adp.benefits.carrier.model.cpm.models.RuleError;
import com.adp.benefits.carrier.service.ProcessStepOutcomeService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;

@Service
@Slf4j
public class EvaluateStepSseService {

    private final IEvaluateStepService evaluateStepService;
    private final Scheduler workerScheduler;
    private final Scheduler timeScheduler;
    private final ProcessStepOutcomeService processStepOutcomeService;

    @Value("${cpm.api-timeouts.evaluate-step:30000ms}")
    private Duration timeout;

    public EvaluateStepSseService(
            IEvaluateStepService evaluateStepService,
            @Qualifier("workerScheduler") Scheduler workerScheduler,
            @Qualifier("timerScheduler") Scheduler timeScheduler,
            ProcessStepOutcomeService processStepOutcomeService) {
        this.evaluateStepService = evaluateStepService;
        this.workerScheduler = workerScheduler;
        this.timeScheduler = timeScheduler;
        this.processStepOutcomeService = processStepOutcomeService;
    }

    public Flux<ServerSentEvent<EvaluateStepResponse>> stream(EvaluateStepRequest request) {
        log.info("Received Request Payload for validation: {}", request.getPayload());

        String cid = String.valueOf(request.getCorrelationId());
        String tid = String.valueOf(request.getTransmissionId());
        String processId = request.getProcessId();
        String processStepId = request.getProcessStepId();
        Instant start = Instant.now();
        JsonNode payload = request.getPayload();

        Map<String, String> mdcCtx = MDC.getCopyOfContextMap();

        Mono<ServerSentEvent<EvaluateStepResponse>> initiatedSse =
                createInitiatedResponse(cid, tid, payload);

        Mono<EvaluateStepResponse> workResponse =
                evaluateStepService
                        .evaluateStep(request)
                        .publishOn(workerScheduler)
                        .map(
                                response ->
                                        response.toBuilder()
                                                .timestamp(Date.from(Instant.now()))
                                                .asyncStatus(
                                                        deriveAsyncStatus(response.getStatus()))
                                                .payload(payload)
                                                .build())
                        .doOnNext(
                                response -> {
                                    long elapsed =
                                            Duration.between(start, Instant.now()).toMillis();
                                    log.info(
                                            "event=evaluateStep.completed cid={} tid={} elapsed={}ms",
                                            cid,
                                            tid,
                                            elapsed);
                                });

        Mono<ServerSentEvent<EvaluateStepResponse>> workSse =
                workResponse
                        .map(this::buildDataSseEvent)
                        .timeout(timeout, timeScheduler)
                        .onErrorResume(
                                TimeoutException.class,
                                ex ->
                                        timeoutResponse(cid, tid, start, timeout.toMillis())
                                                .doOnNext(
                                                        r ->
                                                                processStepOutcomeService
                                                                        .rollbackProcessStepOutcomeForTimeoutAndFailure(
                                                                                cid,
                                                                                tid,
                                                                                processId,
                                                                                processStepId,
                                                                                AsyncStatus
                                                                                        .TIMEOUT))
                                                .map(this::buildDataSseEvent))
                        .onErrorResume(
                                ex ->
                                        logAndWrapFailure(ex, cid, tid, start)
                                                .doOnNext(
                                                        r ->
                                                                processStepOutcomeService
                                                                        .rollbackProcessStepOutcomeForTimeoutAndFailure(
                                                                                cid,
                                                                                tid,
                                                                                processId,
                                                                                processStepId,
                                                                                AsyncStatus.FAILED))
                                                .map(this::buildDataSseEvent));

        return Flux.concat(initiatedSse.flux(), workSse.flux())
                .doFinally(signalType -> logStreamTermination(signalType, mdcCtx, cid, tid));
    }

    private Mono<ServerSentEvent<EvaluateStepResponse>> createInitiatedResponse(
            String cid, String tid, JsonNode payload) {
        log.info(
                "event=evaluateStep.initiated cid={} tid={} timestamp={}", cid, tid, Instant.now());

        EvaluateStepResponse response =
                EvaluateStepResponse.builder()
                        .correlationId(UUID.fromString(cid))
                        .transmissionId(UUID.fromString(tid))
                        .asyncStatus(AsyncStatus.INITIATED)
                        .timestamp(Date.from(Instant.now()))
                        .payload(payload)
                        .build();

        return Mono.just(buildDataSseEvent(response));
    }

    public Mono<EvaluateStepResponse> logAndWrapFailure(
            Throwable ex, String cid, String tid, Instant start) {
        long elapsed = Duration.between(start, Instant.now()).toMillis();
        log.warn(
                "event=evaluateStep.failed cid={} tid={} elapsed={}ms exception={}",
                cid,
                tid,
                elapsed,
                ex.toString());

        RuleError err =
                RuleError.builder()
                        .ruleId("INTERNAL_SERVER_ERROR")
                        .message(ex.getMessage())
                        .errorDetails(
                                java.util.List.of(
                                        Map.of(
                                                "exceptionType", ex.getClass().getSimpleName(),
                                                "details", ex.getMessage())))
                        .build();

        return Mono.just(
                EvaluateStepResponse.builder()
                        .timestamp(Date.from(Instant.now()))
                        .correlationId(UUID.fromString(cid))
                        .transmissionId(UUID.fromString(tid))
                        .asyncStatus(AsyncStatus.FAILED)
                        .status(Status.INTERNAL_SERVER_ERROR)
                        .messages(java.util.List.of(err))
                        .nextStep("")
                        .build());
    }

    public Mono<EvaluateStepResponse> timeoutResponse(
            String cid, String tid, Instant start, long timeoutMs) {
        long elapsed = Duration.between(start, Instant.now()).toMillis();
        log.warn(
                "event=evaluateStep.timeout cid={} tid={} elapsed={}ms timeout={}ms",
                cid,
                tid,
                elapsed,
                timeoutMs);

        RuleError err =
                RuleError.builder()
                        .ruleId("SERVICE_TIMEOUT")
                        .message("Evaluate step api timed out after " + timeoutMs + " ms")
                        .build();

        return Mono.just(
                EvaluateStepResponse.builder()
                        .timestamp(Date.from(Instant.now()))
                        .correlationId(UUID.fromString(cid))
                        .transmissionId(UUID.fromString(tid))
                        .asyncStatus(AsyncStatus.TIMEOUT)
                        .status(Status.TIMEOUT)
                        .messages(java.util.List.of(err))
                        .nextStep("")
                        .build());
    }

    private ServerSentEvent<EvaluateStepResponse> buildDataSseEvent(EvaluateStepResponse response) {
        return ServerSentEvent.<EvaluateStepResponse>builder()
                .id(UUID.randomUUID().toString())
                .event(response.getAsyncStatus().name())
                .data(response)
                .build();
    }

    private void logStreamTermination(
            SignalType signal, Map<String, String> mdcCtx, String cid, String tid) {
        if (mdcCtx != null) MDC.setContextMap(mdcCtx);

        switch (signal) {
            case ON_COMPLETE ->
                    log.info(
                            "SSE stream completed successfully for cid={} tid={} timestamp={}",
                            cid,
                            tid,
                            Instant.now());
            case CANCEL ->
                    log.info(
                            "SSE stream closed by client for cid={} tid={} timestamp={} (expected after final event).",
                            cid,
                            tid,
                            Instant.now());
            default ->
                    log.warn(
                            "SSE stream terminated with signal {} for cid={} tid={} timestamp={}",
                            signal,
                            cid,
                            tid,
                            Instant.now());
        }

        MDC.clear();
    }

    private AsyncStatus deriveAsyncStatus(Status status) {
        return switch (status) {
            case TIMEOUT -> AsyncStatus.TIMEOUT;
            case INTERNAL_SERVER_ERROR -> AsyncStatus.FAILED;
            default -> AsyncStatus.COMPLETED;
        };
    }
}
