package com.adp.benefits.carrier.model.cpm.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStepRequest {
    private UUID transmissionId;
    private String aoid; // AssociateOID, X-OID
    private String orgId; // ClientId, OrgOID, OrgId
    private String systemId;
    private String processId;
    private String processStepId;
    private JsonNode payload;
}
