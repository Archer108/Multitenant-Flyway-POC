package com.adp.benefits.carrier.api.service.rule;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class RuleMappingKey {
    private final String template;
    private final String jsonPointer;
    private final String leafPointer;

    public RuleMappingKey(String template, AttributeMappingLoader.MappingEntry me) {
        this.template = template;
        this.jsonPointer = me.getJsonPointer();
        this.leafPointer = me.getLeafPointer();
    }
}
