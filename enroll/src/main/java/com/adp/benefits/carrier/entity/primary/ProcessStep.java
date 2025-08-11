package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "process_step", schema = "bcpm_primary_schema")
@EqualsAndHashCode(exclude = {"processEntity", "attributeAssociations", "clientStepOverrides"})
public class ProcessStep {

    @Id
    @Column(name = "process_step_id", nullable = false)
    private String processStepId;

    @Column(name = "name_tx", nullable = false)
    @NotNull(message = "Process step name cannot be null")
    private String name;

    @Column(name = "async_in", nullable = false)
    @NotNull(message = "Async in is required")
    private boolean asyncIn;

    @Column(name = "step_order", nullable = false)
    @NotNull(message = "Step Order is required")
    private int stepOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "process_id", nullable = false)
    private ProcessEntity processEntity;

    @OneToMany(
            mappedBy = "processStep",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ProcStepAttrAssoc> attributeAssociations;

    @OneToMany(
            mappedBy = "overrideStep",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ClientStepOverride> clientStepOverrides;
}
