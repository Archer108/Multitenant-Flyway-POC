package com.adp.benefits.carrier.api.service.rule;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;

public class AttributeMappingLoader {

    @Getter
    public static class MappingEntry {
        private String jsonPointer;
        private String leafPointer;
        private String label;
        private String regex;

        // Jackson requires a no‑arg constructor + setters, or use Lombok @Data
    }

    private final Map<String, List<MappingEntry>> mappings;

    public AttributeMappingLoader(Map<String, List<MappingEntry>> mappings) {
        this.mappings = mappings;
    }

    /** All configured entries for a given attributeId. */
    public List<MappingEntry> getEntries(String attributeId) {
        return mappings.getOrDefault(attributeId, Collections.emptyList());
    }

    /** Only those entries whose jsonPointer actually resolves in this payload. */
    public List<MappingEntry> getPresentEntries(String attributeId, JsonNode payload) {
        return getEntries(attributeId).stream()
                .filter(me -> !payload.at(me.getJsonPointer()).isMissingNode())
                .toList();
    }
}
