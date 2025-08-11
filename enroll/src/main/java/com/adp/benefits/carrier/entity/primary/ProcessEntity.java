package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "process", schema = "bcpm_primary_schema")
@EqualsAndHashCode(exclude = {"systemEntity", "processSteps", "processClientAssociations"})
public class ProcessEntity {

    @Id
    @Column(name = "process_id", nullable = false)
    private String processId;

    @Column(name = "name_tx", nullable = false)
    @NotNull(message = "Process name cannot be null")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemEntity systemEntity;

    @OneToMany(
            mappedBy = "processEntity",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private List<ProcessStep> processSteps;

    @OneToMany(
            mappedBy = "processEntity",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private List<ProcessClientAssoc> processClientAssociations;
}
