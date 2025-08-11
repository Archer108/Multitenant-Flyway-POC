package com.adp.benefits.carrier.entity.client;

import com.adp.benefits.carrier.model.Auditable;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "process_step_outcome", schema = "bcpm_client_schema")
public class ProcessStepOutcome extends Auditable {

    @Id
    @Column(name = "process_step_outcome_id")
    private String processStepOutcomeId;

    @Column(name = "correlation_id", nullable = false)
    @NotNull(message = "Correlation Id is required")
    private String correlationId;

    @Column(name = "process_step_id", nullable = false)
    @NotNull(message = "Process Step ID is required")
    private String processStepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_tx", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_tx")
    private State state;

    @Column(name = "resume_in")
    private Boolean resume;

    @Column(name = "next_step_id")
    private String nextStepId;

    @Column(name = "override_process_step_outcome_id")
    private String overrideProcessStepOutcomeId;

    @Column(name = "startingTimelineDate_dt")
    private Date startingTimelineDate;

    @Column(name = "aoid")
    private String aoid;

    @Column(name = "client_id", nullable = false)
    @NotNull(message = "Client Id is required")
    private String clientId;

    @Column(name = "transmission_id", nullable = false)
    @NotNull(message = "Transmission Id is required")
    private String transmissionId;

    @Builder.Default
    @OneToMany(
            mappedBy = "processStepOutcome",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @ToString.Exclude
    private Set<RemediationSupport> remediationSupports = new HashSet<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "processStepOutcome",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @ToString.Exclude
    private Set<Message> messages = new HashSet<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "processStepOutcome",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @ToString.Exclude
    private Set<EnrollmentInfo> enrollmentInfos = new HashSet<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "processStepOutcome",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @ToString.Exclude
    private Set<ActionContext> actionContexts = new HashSet<>();
}
