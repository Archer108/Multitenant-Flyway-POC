package com.adp.benefits.carrier.model.enrollment;

import java.util.List;
import lombok.Value;

/** Immutable container for per-tenant results. */
@Value
public class EnrollmentInfoTenantResult {
    List<String> carriers;
    List<PsoDataDto> psoData;
}
