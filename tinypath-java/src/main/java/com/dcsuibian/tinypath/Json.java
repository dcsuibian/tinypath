package com.dcsuibian.tinypath;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class Json {
    static final JsonMapper MAPPER = JsonMapper.builder().build();

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
