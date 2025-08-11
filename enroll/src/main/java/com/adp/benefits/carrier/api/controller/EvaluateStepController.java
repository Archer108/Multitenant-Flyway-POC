package com.adp.benefits.carrier.api.controller;

import com.adp.benefits.carrier.api.interfaces.IEvaluateStep;
import com.adp.benefits.carrier.api.service.EvaluateStepSseService;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class EvaluateStepController implements IEvaluateStep {

    private final EvaluateStepSseService sseService;

    @Override
    public ResponseEntity<Flux<ServerSentEvent<EvaluateStepResponse>>> evaluateStep(
            @Valid EvaluateStepRequest req) {

        Flux<ServerSentEvent<EvaluateStepResponse>> stream = sseService.stream(req);

        return ResponseEntity.status(HttpStatus.ACCEPTED) // 202 on handshake
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .body(stream);
    }
}
