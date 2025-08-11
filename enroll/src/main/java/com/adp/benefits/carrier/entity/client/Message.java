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
@Table(name = "message", schema = "bcpm_client_schema")
public class Message extends Auditable {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "process_step_outcome_id", nullable = false)
    @NotNull(message = "process_step_outcome_id is required")
    private String processStepOutcomeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "message", nullable = false)
    @NotNull(message = "Message is required")
    private JsonNode message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_step_outcome_id", insertable = false, updatable = false)
    @ToString.Exclude
    private ProcessStepOutcome processStepOutcome;
}
