package com.adp.benefits.carrier.api.service.rule.config;

import com.adp.benefits.carrier.api.service.rule.AttributeMappingLoader;
import com.adp.benefits.carrier.api.service.rule.AttributeMappingLoader.MappingEntry;
import com.adp.benefits.carrier.exceptions.AttributeMappingConfigurationException;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AttributeMappingConfig {

    private static final String RESOURCE = "attributeMappings.json";

    /**
     * Load & validate the mapping file into a Map<attributeId, List<MappingEntry>>. If anything is
     * wrong (missing file, bad JSON‐pointer, invalid regex, missing label), we throw an
     * AttributeMappingConfigurationException at startup.
     */
    @Bean
    public Map<String, List<MappingEntry>> attributeMappings(ObjectMapper mapper) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {

            if (in == null) {
                throw new AttributeMappingConfigurationException(
                        RESOURCE + " not found on classpath");
            }
            JsonNode root = mapper.readTree(in);
            Map<String, List<MappingEntry>> result = new LinkedHashMap<>();

            for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> fld = it.next();
                String attributeId = fld.getKey();
                JsonNode node = fld.getValue();

                List<MappingEntry> entries = new ArrayList<>();
                if (node.isArray()) {
                    for (JsonNode elem : node) {
                        entries.add(parseAndValidate(elem, mapper));
                    }
                } else if (node.isObject()) {
                    entries.add(parseAndValidate(node, mapper));
                } else {
                    throw new AttributeMappingConfigurationException(
                            "Invalid mapping format for key: " + attributeId);
                }

                result.put(attributeId, Collections.unmodifiableList(entries));
            }

            return Collections.unmodifiableMap(result);

        } catch (AttributeMappingConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AttributeMappingConfigurationException(
                    "Failed to load/validate " + RESOURCE, ex);
        }
    }

    /**
     * Wraps the raw JSON → MappingEntry and verifies: • jsonPointer compiles • leafPointer compiles
     * when non‑empty • label is non‑blank • regex compiles when non‑blank
     */
    private MappingEntry parseAndValidate(JsonNode node, ObjectMapper mapper) {
        MappingEntry me = mapper.convertValue(node, MappingEntry.class);

        // jsonPointer syntax
        try {
            JsonPointer.compile(me.getJsonPointer());
        } catch (IllegalArgumentException iae) {
            throw new AttributeMappingConfigurationException(
                    "Invalid jsonPointer '" + me.getJsonPointer() + "'", iae);
        }

        // leafPointer syntax (optional)
        if (me.getLeafPointer() != null && !me.getLeafPointer().isBlank()) {
            try {
                JsonPointer.compile(me.getLeafPointer());
            } catch (IllegalArgumentException iae) {
                throw new AttributeMappingConfigurationException(
                        "Invalid leafPointer '" + me.getLeafPointer() + "'", iae);
            }
        }

        // label
        if (me.getLabel() == null || me.getLabel().isBlank()) {
            throw new AttributeMappingConfigurationException("MappingEntry.label must be set");
        }

        // regex
        if (me.getRegex() != null && !me.getRegex().isBlank()) {
            try {
                Pattern.compile(me.getRegex());
            } catch (PatternSyntaxException pse) {
                throw new AttributeMappingConfigurationException(
                        "Invalid regex '" + me.getRegex() + "'", pse);
            }
        }

        return me;
    }

    /** Converts that validated map into an injectable loader bean. */
    @Bean
    public AttributeMappingLoader attributeMappingLoader(
            Map<String, List<MappingEntry>> attributeMappings) {
        return new AttributeMappingLoader(attributeMappings);
    }
}
