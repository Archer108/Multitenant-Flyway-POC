package com.adp.benefits.carrier.api.service.interfaces;

import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoPageEnvelope;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoRequest;
import reactor.core.publisher.Mono;

/**
 * Reactive façade so the controller stays non-blocking while blocking JPA queries run on a worker
 * Scheduler.
 */
public interface IEnrollmentInfoService {
    Mono<EnrollmentInfoPageEnvelope> getEnrollmentInfoCursor(
            EnrollmentInfoRequest request, String cursor, Integer size);
}
