package com.dcsuibian.tinypath;

import tools.jackson.databind.JsonNode;

import java.util.List;

public final class TinyPath {
    private final String expression;
    private final List<Step> steps;

    private TinyPath(String expression, List<Step> steps) {
        this.expression = expression;
        this.steps = steps;
    }

    /**
     * Compiles a TinyPath expression. Throws {@link TinyPathException} on syntax errors.
     */
    public static TinyPath compile(String expression) {
        List<Step> steps = Parser.parse(expression);
        return new TinyPath(expression, steps);
    }

    /**
     * Evaluates this expression against the given JSON string and converts the result
     * to the specified type.
     * <p>
     * Returns {@code null} when the path does not resolve (missing field, out-of-bounds
     * index, or no filter match). Throws {@link TinyPathException} on syntax errors or
     * invalid JSON input.
     */
    public <T> T evaluate(String json, Class<T> type) {
        JsonNode root = Json.parse(json);
        JsonNode result = Evaluator.evaluate(steps, root);
        return Json.convert(result, type);
    }


    @Override
    public String toString() {
        return expression;
    }
}
