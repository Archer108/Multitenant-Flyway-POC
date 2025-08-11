package com.adp.benefits.carrier.model.enrollment;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PsoDataDto {
    private String systemType;
    private String clientName;
    private String carrierName;
    private String transmissionId;
    private String correlationId;
    private AssociateDto associateDto;
    private String status;
    private String state;
    private Date occurredOn; // created_on_dt as Date
    private List<String> planTypes;
    private List<MessageDto> messages;
}
