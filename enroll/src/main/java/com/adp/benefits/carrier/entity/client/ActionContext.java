package com.adp.benefits.carrier.entity.client;

import com.adp.benefits.carrier.model.Auditable;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "action_context", schema = "bcpm_client_schema")
public class ActionContext extends Auditable {

    @Id
    @Column(name = "transmission_id", nullable = false)
    @NotNull(message = "Transmission ID is required")
    private String transmissionId;

    @Column(name = "process_step_outcome_id")
    @NotNull(message = "Process step outcome ID is required")
    private String processStepOutcomeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private JsonNode payload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_step_outcome_id", insertable = false, updatable = false)
    @ToString.Exclude
    private ProcessStepOutcome processStepOutcome;
}
