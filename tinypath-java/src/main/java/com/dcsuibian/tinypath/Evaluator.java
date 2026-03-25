package com.dcsuibian.tinypath;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

import java.util.List;

class Evaluator {

    static JsonNode evaluate(List<Step> steps, JsonNode root) {
        JsonNode current = root;
        for (Step step : steps) {
            if (null == current || current.isNull() || current.isMissingNode()) {
                return NullNode.instance;
            }
            if (step instanceof FieldStep s) {
                current = evaluateField(s, current);
            } else if (step instanceof IndexStep s) {
                current = evaluateIndex(s, current);
            } else if (step instanceof FilterStep s) {
                current = evaluateFilter(s, current);
            }
        }
        return current;
    }

    private static JsonNode evaluateField(FieldStep step, JsonNode current) {
        if (!current.isObject()) {
            return NullNode.instance;
        }
        JsonNode result = current.get(step.name());
        return null != result ? result : NullNode.instance;
    }

    private static JsonNode evaluateIndex(IndexStep step, JsonNode current) {
        if (!current.isArray()) {
            return NullNode.instance;
        }
        if (step.index() >= current.size()) {
            return NullNode.instance;
        }
        return current.get(step.index());
    }

    private static JsonNode evaluateFilter(FilterStep step, JsonNode current) {
        if (!current.isArray()) {
            return NullNode.instance;
        }
        for (JsonNode element : current) {
            if (!element.isObject()) {
                continue;
            }
            boolean matches = step.conditions().stream().allMatch(c -> matchesCondition(c, element));
            if (matches) {
                return element;
            }
        }
        return NullNode.instance;
    }

    private static boolean matchesCondition(Condition condition, JsonNode element) {
        JsonNode actual = element.get(condition.key());
        Object expected = condition.value();
        if (null == actual || actual.isMissingNode()) {
            return null == expected;
        }
        if (null == expected) {
            return actual.isNull();
        }
        if (expected instanceof String s) {
            return actual.isString() && s.equals(actual.stringValue());
        }
        if (expected instanceof Boolean b) {
            return actual.isBoolean() && b == actual.booleanValue();
        }
        if (expected instanceof Number expNum) {
            if (!actual.isNumber()) {
                return false;
            }
            Number actualNum = actual.numberValue();
            if (expected instanceof Double) {
                return expNum.doubleValue() == actualNum.doubleValue();
            }
            return expNum.longValue() == actualNum.longValue();
        }
        return false;
    }
}
