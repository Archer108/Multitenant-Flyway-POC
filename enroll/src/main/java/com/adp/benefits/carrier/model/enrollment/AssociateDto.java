package com.adp.benefits.carrier.model.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AssociateDto {
    private String fullName;
    private String associateId;
    private String associateDetailsLink;
}
