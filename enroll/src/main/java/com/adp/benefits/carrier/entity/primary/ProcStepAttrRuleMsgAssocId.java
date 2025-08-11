package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ProcStepAttrRuleMsgAssocId implements Serializable {

    @Column(name = "proc_step_attr_rule_assoc_id", nullable = false)
    private String procStepAttrRuleAssocId;

    @Column(name = "message_id", nullable = false)
    private String messageId;
}
