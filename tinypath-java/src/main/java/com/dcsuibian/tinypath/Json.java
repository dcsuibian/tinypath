package com.dcsuibian.tinypath;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class Json {
    static final ObjectMapper MAPPER = new ObjectMapper();

    static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new TinyPathException("Invalid JSON: " + e.getMessage());
        }
    }

    static <T> T convert(JsonNode node, Class<T> type) {
        if (null == node || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return MAPPER.convertValue(node, type);
    }
}
