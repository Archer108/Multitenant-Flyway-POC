package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "rule", schema = "bcpm_primary_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"defaultMessage", "ruleAssociations"})
@ToString(exclude = {"defaultMessage", "ruleAssociations"})
public class Rule {

    @Id
    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(name = "name_tx", nullable = false)
    @NotNull(message = "Rule name cannot be null")
    private String name;

    @Column(name = "rule_cond_id", nullable = false)
    @NotNull(message = "Rule condition ID cannot be null")
    private String ruleCondId;

    @Column(name = "eff_start_date_dt")
    @Temporal(TemporalType.DATE)
    private Date effStartDate;

    @Column(name = "eff_until_date_dt")
    @Temporal(TemporalType.DATE)
    private Date effUntilDate;

    @Column(name = "rule_expression")
    private String ruleExpression;

    @Column(name = "enabled_in", nullable = false)
    private boolean enabledIn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_id", nullable = false)
    private PrimaryMessage defaultMessage;

    @OneToMany(
            mappedBy = "rule",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ProcStepAttrRuleAssoc> procStepAttrRuleAssocs;
}
