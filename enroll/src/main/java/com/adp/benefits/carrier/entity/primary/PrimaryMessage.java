package com.adp.benefits.carrier.entity.primary;

import com.adp.benefits.carrier.enums.RuleExecutionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "message", schema = "bcpm_primary_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"rules", "procStepAttrRuleMsgAssocs"})
@ToString(exclude = {"rules", "procStepAttrRuleMsgAssocs"})
public class PrimaryMessage {

    @Id
    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_tx", nullable = false)
    @NotNull(message = "Message type cannot be null")
    private RuleExecutionStatus type;

    @Column(name = "body_tx", nullable = false)
    @NotNull(message = "Message body cannot be null")
    private String body;

    @Column(name = "eff_start_date_dt")
    @Temporal(TemporalType.DATE)
    private Date effStartDate;

    @Column(name = "eff_until_date_dt")
    @Temporal(TemporalType.DATE)
    private Date effUntilDate;

    @Column(name = "enabled_in", nullable = false)
    private boolean enabled;

    @OneToMany(
            mappedBy = "defaultMessage",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<Rule> rules;

    @OneToMany(
            mappedBy = "primaryMessage",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ProcStepAttrRuleMsgAssoc> procStepAttrRuleMsgAssocs;
}
