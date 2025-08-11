package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "client_step_override", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"overriddenStep", "overrideStep"})
public class ClientStepOverride {

    @EmbeddedId private ClientStepOverrideId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("clientId")
    @JoinColumn(name = "client_id", nullable = false)
    @NotNull(message = "Client cannot be null")
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("overrideStepId")
    @JoinColumn(name = "override_step_id", nullable = false)
    @NotNull(message = "Override step cannot be null")
    private ProcessStep overrideStep;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "overriden_step_id", nullable = false)
    @NotNull(message = "Overridden step cannot be null")
    private ProcessStep overriddenStep;

    @Column(name = "pause_in", nullable = false)
    private boolean pauseIn;

    @Column(name = "skip_in", nullable = false)
    private boolean skipIn;
}
