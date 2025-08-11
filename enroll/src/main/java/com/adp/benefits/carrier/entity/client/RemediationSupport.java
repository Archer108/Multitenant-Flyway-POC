package com.adp.benefits.carrier.entity.client;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "remediation_support", schema = "bcpm_client_schema")
public class RemediationSupport {

    @Id
    @Column(name = "remediation_policy_id", nullable = false)
    @NotNull(message = "Remediation Policy Id is required")
    private String remediationPolicyId;

    @Column(name = "process_step_outcome_id")
    private String processStepOutcomeId;

    @Column(name = "support_plan_tx", nullable = false)
    @NotNull(message = "Support Plan is required")
    private String supportPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "process_step_outcome_id",
            insertable = false,
            updatable = false,
            nullable = false)
    @ToString.Exclude
    private ProcessStepOutcome processStepOutcome;
}
