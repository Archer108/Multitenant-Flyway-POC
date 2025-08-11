package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "proc_step_attr_rule_msg_assoc", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"procStepAttrRuleAssoc", "primaryMessage"})
public class ProcStepAttrRuleMsgAssoc {

    @EmbeddedId private ProcStepAttrRuleMsgAssocId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("procStepAttrRuleAssocId")
    @JoinColumn(name = "proc_step_attr_rule_assoc_id", nullable = false)
    @NotNull(message = "Process step attribute rule association cannot be null")
    private ProcStepAttrRuleAssoc procStepAttrRuleAssoc;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("messageId")
    @JoinColumn(name = "message_id", nullable = false)
    @NotNull(message = "Message cannot be null")
    private PrimaryMessage primaryMessage;
}
