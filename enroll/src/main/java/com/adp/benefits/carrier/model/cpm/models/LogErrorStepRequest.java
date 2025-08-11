package com.adp.benefits.carrier.model.cpm.models;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LogErrorStepRequest {
    private UUID correlationId; // CorrelationID, X-Correlation-ID
    private UUID transmissionId; // Nullable
    private String aoid; // AssociateOID, X-OID
    private String orgId; // ClientId, OrgOID, OrgId
    private String systemId;
    private String processId;
    private String processStepId;
    private ErrorInfo errorInfo;
}
