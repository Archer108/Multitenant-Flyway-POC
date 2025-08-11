package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "proc_step_attr_rule_assoc", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"procStepAttrAssoc", "rule", "procStepAttrRuleMsgAssocs"})
public class ProcStepAttrRuleAssoc {

    @Id
    @Column(name = "proc_step_attr_rule_assoc_id")
    private String procStepAttrRuleAssocId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proc_step_attr_assoc_id", nullable = false)
    @NotNull(message = "Process step attribute association cannot be null")
    private ProcStepAttrAssoc procStepAttrAssoc;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id", nullable = false)
    @NotNull(message = "Rule cannot be null")
    private Rule rule;

    @Column(name = "enabled_in", nullable = false)
    private boolean enabledIn;

    @OneToMany(
            mappedBy = "procStepAttrRuleAssoc",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ProcStepAttrRuleMsgAssoc> procStepAttrRuleMsgAssocs;
}
