package com.adp.benefits.carrier.api.service.interfaces;

import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoRequest;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoResponse;
import reactor.core.publisher.Mono;

public interface IEnrollmentInfoStepService {
    Mono<EnrollmentInfoResponse> getEnrollmentInfo(EnrollmentInfoRequest request);
}
