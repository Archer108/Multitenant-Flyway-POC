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
@Table(name = "proc_step_attr_assoc", schema = "bcpm_primary_schema")
@EqualsAndHashCode(exclude = {"attribute", "processStep", "procStepAttrRuleAssocs"})
public class ProcStepAttrAssoc {

    @Id
    @Column(name = "proc_step_attr_assoc_id", nullable = false)
    private String procStepAttrAssocId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attribute_id", nullable = false)
    @NotNull(message = "Attribute is required")
    private Attribute attribute;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "process_step_id", nullable = false)
    @NotNull(message = "Process step is required")
    private ProcessStep processStep;

    @Column(name = "enabled_in", nullable = false)
    private boolean enabledIn;

    @OneToMany(
            mappedBy = "procStepAttrAssoc",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private List<ProcStepAttrRuleAssoc> procStepAttrRuleAssocs;
}
