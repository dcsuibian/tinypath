package com.dcsuibian.tinypath;

import java.util.ArrayList;
import java.util.List;

class Parser {
    private final String expression;
    private int pos;

    private Parser(String expression) {
        this.expression = expression;
        this.pos = 0;
    }

    static List<Step> parse(String expression) {
        return new Parser(expression).doParse();
    }

    private List<Step> doParse() {
        expect('$');
        List<Step> steps = new ArrayList<>();
        while (pos < expression.length()) {
            steps.add(parseStep());
        }
        return steps;
    }

    private Step parseStep() {
        expect('[');
        char c = peek();
        Step step;
        if ('"' == c) {
            step = parseFieldStep();
        } else if ('?' == c) {
            step = parseFilterStep();
        } else if (Character.isDigit(c)) {
            step = parseIndexStep();
        } else {
            throw new TinyPathException("Unexpected character '" + c + "' at position " + pos);
        }
        expect(']');
        return step;
    }

    // ["fieldName"]
    private FieldStep parseFieldStep() {
        String name = parseQuotedString();
        return new FieldStep(name);
    }

    // [n]
    private IndexStep parseIndexStep() {
        int start = pos;
        while (pos < expression.length() && Character.isDigit(expression.charAt(pos))) {
            pos++;
        }
        String numStr = expression.substring(start, pos);
        try {
            return new IndexStep(Integer.parseInt(numStr));
        } catch (NumberFormatException e) {
            throw new TinyPathException("Invalid array index: " + numStr);
        }
    }

    // [?(@["key"]==value&&...)]
    private FilterStep parseFilterStep() {
        expect('?');
        expect('(');
        List<Condition> conditions = new ArrayList<>();
        conditions.add(parseCondition());
        while (pos < expression.length() && '&' == peek()) {
            expect('&');
            expect('&');
            conditions.add(parseCondition());
        }
        expect(')');
        return new FilterStep(conditions);
    }

    // @["key"]==value
    private Condition parseCondition() {
        expect('@');
        expect('[');
        String key = parseQuotedString();
        expect(']');
        expect('=');
        expect('=');
        Object value = parseFilterValue();
        return new Condition(key, value);
    }

    private Object parseFilterValue() {
        char c = peek();
        if ('"' == c) {
            return parseQuotedString();
        } else if ('t' == c) {
            expectLiteral("true");
            return Boolean.TRUE;
        } else if ('f' == c) {
            expectLiteral("false");
            return Boolean.FALSE;
        } else if ('n' == c) {
            expectLiteral("null");
            return null;
        } else if (Character.isDigit(c) || '-' == c) {
            return parseNumber();
        }
        throw new TinyPathException("Invalid filter value at position " + pos);
    }

    private Number parseNumber() {
        int start = pos;
        if ('-' == peek()) pos++;
        while (pos < expression.length() && Character.isDigit(expression.charAt(pos))) {
            pos++;
        }
        boolean isFloat = pos < expression.length() && '.' == expression.charAt(pos);
        if (isFloat) {
            pos++;
            while (pos < expression.length() && Character.isDigit(expression.charAt(pos))) {
                pos++;
            }
        }
        String numStr = expression.substring(start, pos);
        try {
            return isFloat ? Double.parseDouble(numStr) : Long.parseLong(numStr);
        } catch (NumberFormatException e) {
            throw new TinyPathException("Invalid number: " + numStr);
        }
    }

    private String parseQuotedString() {
        expect('"');
        int start = pos;
        while (pos < expression.length() && '"' != expression.charAt(pos)) {
            pos++;
        }
        String value = expression.substring(start, pos);
        expect('"');
        return value;
    }

    private void expect(char expected) {
        if (pos >= expression.length()) {
            throw new TinyPathException("Unexpected end of expression, expected '" + expected + "'");
        }
        char actual = expression.charAt(pos);
        if (expected != actual) {
            throw new TinyPathException("Expected '" + expected + "' but got '" + actual + "' at position " + pos);
        }
        pos++;
    }

    private void expectLiteral(String literal) {
        if (!expression.startsWith(literal, pos)) {
            throw new TinyPathException("Expected '" + literal + "' at position " + pos);
        }
        pos += literal.length();
    }

    private char peek() {
        if (pos >= expression.length()) {
            throw new TinyPathException("Unexpected end of expression");
        }
        return expression.charAt(pos);
    }
}
