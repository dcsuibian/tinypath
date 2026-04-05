package com.dcsuibian.tinypath;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class TinyPathTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @TestFactory
    Collection<DynamicTest> compliance() throws Exception {
        String path = System.getProperty("tinypath.tests");
        assertNotNull(path, "System property 'tinypath.tests' is not set");

        JsonNode cases = MAPPER.readTree(new File(path));
        Collection<DynamicTest> tests = new ArrayList<>();

        for (JsonNode tc : cases) {
            String description = tc.get("description").stringValue();
            String expression = tc.get("expression").stringValue();
            boolean invalid = tc.has("invalid") && tc.get("invalid").booleanValue();

            if (invalid) {
                tests.add(dynamicTest(description, () -> {
                    System.out.println("  [invalid] " + description);
                    assertThrows(TinyPathException.class, () -> TinyPath.compile(expression));
                }));
            } else {
                String data = MAPPER.writeValueAsString(tc.get("data"));
                JsonNode expected = tc.get("expected");
                tests.add(dynamicTest(description, () -> {
                    System.out.println("  [valid]   " + description);
                    Object result = TinyPath.compile(expression).evaluate(data, Object.class);
                    assertEquals(expected, MAPPER.valueToTree(result));
                }));
            }
        }

        return tests;
    }
}
