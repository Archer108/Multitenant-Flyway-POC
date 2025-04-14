package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.exceptions.AttributeMappingConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

public class AttributeMappingLoader {

    private AttributeMappingLoader() {
        throw new UnsupportedOperationException(
                "AttributeMappingLoader is a Utility class and should not be instantiated");
    }

    private static final Map<String, Map<String, String>> mappings = new HashMap<>();

    static {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in =
                     AttributeMappingLoader.class
                             .getClassLoader()
                             .getResourceAsStream("attribute-mappings.json")) {
            System.out.println("📂 Attribute Mapping File Loaded? " + (in != null));
            if (in != null) {
                Map<String, Map<String, String>> map =
                        mapper.readValue(in, new TypeReference<>() {});
                mappings.putAll(map);
            }
        } catch (Exception e) {
            throw new AttributeMappingConfigurationException(
                    "Unable to load attribute mapping configuration", e);
        }
    }

    public static String getMapping(String attributeId, String rootKey) {
        Map<String, String> innerMap = mappings.get(attributeId);
        return (innerMap != null) ? innerMap.get(rootKey) : null;
    }


    public static List<String> mapAttributesPaths(String attributeId, Map<String, Object> rootMap) {
        Set<String> foundPaths = new HashSet<>();
        Map<String, String> innerMap = mappings.get(attributeId);

        System.out.println("🧩 Evaluating attribute: " + attributeId);
        System.out.println("📂 Available payload root keys: " + rootMap.keySet());
        System.out.println("📋 Expected paths: " + (innerMap != null ? innerMap.keySet() : "null"));

        if (innerMap != null) {
            for (String path : innerMap.keySet()) {
                boolean result = containsKeyRecursive(rootMap, path);
                System.out.println("🔍 containsKeyRecursive('" + path + "') = " + result);
                if (result) {
                    foundPaths.add(path);
                }
            }
        }
        return new ArrayList<>(foundPaths);
    }

    private static boolean containsKeyRecursive(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            System.out.println("✅ Found key directly in map: " + key);
            return true;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nestedMap) {
                @SuppressWarnings("unchecked")
                boolean found = containsKeyRecursive((Map<String, Object>) nestedMap, key);
                if (found) {
                    System.out.println("🔁 Found in nested map under key: " + entry.getKey());
                    return true;
                }
            }

            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> listMap) {
                        @SuppressWarnings("unchecked")
                        boolean found = containsKeyRecursive((Map<String, Object>) listMap, key);
                        if (found) {
                            System.out.println("🧮 Found in list under: " + entry.getKey());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
