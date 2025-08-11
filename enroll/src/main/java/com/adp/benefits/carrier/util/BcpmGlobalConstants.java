package com.adp.benefits.carrier.util;

public class BcpmGlobalConstants {

    private BcpmGlobalConstants() {}

    public static final String TENANT_ID = "tenantId";
    public static final String UNKNOWN_TENANT = "UNKNOWN";

    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ERRORED = "ERRORED";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    public static final String ERROR_KEY = "error";
    public static final String ATTRIBUTE_NAME_PLACEHOLDER = "{attributeName}";
    public static final String JSON_POINTER_PLACEHOLDER = "{jsonPointer}";
    public static final String DEFAULT_UNKNOWN_ATTRIBUTE = "Unknown Attribute";
    public static final String ERROR_NEXT_STEP_ID = "some_fix_error_next_step_id";
    public static final String PROCESS_COMPLETED = "process_completed";

    public static final String OVERRIDE_CLIENT = "overrideClient";
    public static final String OVERRIDE_STEP = "overrideStep";

    public static final String PAYLOAD_VARIABLE = "payload";
    public static final String EMPTY_STRING = "";

    public static final String PAYLOAD_RULE_JSON_POINTER_PLACEHOLDER = "#payload{jsonPointer}";
    public static final String PAYLOAD_RULE_LEAF_POINTER_PLACEHOLDER = "#this{leafPointer}";
    public static final String THIS_REFERENCE = "#this";
    public static final String PAYLOAD_REFERENCE = "#payload";

    public static final String PAYLOAD_RULE_LABEL_PLACEHOLDER = "{label}";
    public static final String PAYLOAD_RULE_REGEX_PLACEHOLDER = "{regex}";
    public static final String DEFAULT_REGEX = ".*";
    public static final String ATTRIBUTE_MAPPINGS_FILE = "attributeMappings.json";

    public static final String MISCELLANEOUS_CARRIER = "MISCELLANEOUS";
}
