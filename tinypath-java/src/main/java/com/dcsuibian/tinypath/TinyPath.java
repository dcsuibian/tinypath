package com.dcsuibian.tinypath;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * A compiled TinyPath expression that can be evaluated against JSON data.
 *
 * <p>TinyPath is a minimal JSON path expression language designed for IoT scenarios.
 * Expressions are compiled once and can be evaluated repeatedly against different inputs.
 *
 * <pre>{@code
 * TinyPath path = TinyPath.compile("$[\"sensors\"][0][\"value\"]");
 * String value = path.evaluate(json, String.class);
 * }</pre>
 */
public final class TinyPath {
    private final String expression;
    private final List<Step> steps;

    private TinyPath(String expression, List<Step> steps) {
        this.expression = expression;
        this.steps = steps;
    }

    /**
     * Compiles a TinyPath expression.
     *
     * @param expression the TinyPath expression string
     * @return a compiled {@code TinyPath} instance
     * @throws TinyPathException if the expression contains a syntax error
     */
    public static TinyPath compile(String expression) {
        List<Step> steps = Parser.parse(expression);
        return new TinyPath(expression, steps);
    }

    /**
     * Evaluates this expression against the given JSON string and converts the result
     * to the specified type.
     *
     * <p>Returns {@code null} when the path does not resolve — for example, when a field
     * is missing, an array index is out of bounds, or a filter has no match.
     *
     * <p>If you need to evaluate multiple expressions against the same JSON data, prefer
     * {@link #evaluate(JsonNode, Class)} with a pre-parsed {@link JsonNode} to avoid
     * parsing the JSON string repeatedly.
     *
     * @param <T>  the expected result type
     * @param json the JSON string to evaluate against
     * @param type the expected result type class (e.g. {@code String.class}, {@code Integer.class})
     * @return the value at the path, or {@code null} if the path does not resolve
     * @throws TinyPathException if the JSON string is invalid
     */
    public <T> T evaluate(String json, Class<T> type) {
        return evaluate(Json.parse(json), type);
    }

    /**
     * Evaluates this expression against a pre-parsed {@link JsonNode} and converts the result
     * to the specified type.
     *
     * <p>Use this overload when evaluating multiple expressions against the same JSON data,
     * so the JSON string is parsed only once via {@link Json#MAPPER}.
     *
     * <p>Returns {@code null} when the path does not resolve — for example, when a field
     * is missing, an array index is out of bounds, or a filter has no match.
     *
     * @param <T>  the expected result type
     * @param root the pre-parsed JSON tree to evaluate against
     * @param type the expected result type class (e.g. {@code String.class}, {@code Integer.class})
     * @return the value at the path, or {@code null} if the path does not resolve
     */
    public <T> T evaluate(JsonNode root, Class<T> type) {
        JsonNode result = Evaluator.evaluate(steps, root);
        return Json.convert(result, type);
    }

    /**
     * Returns the original expression string.
     */
    @Override
    public String toString() {
        return expression;
    }
}
