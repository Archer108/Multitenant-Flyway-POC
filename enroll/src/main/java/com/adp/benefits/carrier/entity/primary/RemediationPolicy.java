package com.adp.benefits.carrier.entity.primary;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "remediation_policy", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationPolicy {

    @Id
    @Column(name = "remediation_policy_id", nullable = false)
    private String remediationPolicyId;

    @Column(name = "name_tx", nullable = false)
    @NotNull(message = "Policy name cannot be null")
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy", nullable = false)
    private JsonNode policy;
}
