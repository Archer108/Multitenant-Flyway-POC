package com.adp.benefits.carrier.model.enrollment;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentInfoResponse {
    private List<String> clients;
    private List<String> carriers;
    private Timeframe duration;
    private List<PsoDataDto> psoData;
}
