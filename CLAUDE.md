# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

TinyPath is a minimal JSON query language specification — a strict subset of JSONPath — designed for IoT device MQTT message data point mapping. This repository is a **spec repository** containing the language specification and compliance test case definitions.

## Language & Localization Conventions

The author is Chinese, so Chinese may appear in some parts of the project. The general rule is: **prefer English, tolerate Chinese where necessary**.

- **Documents**: bilingual files are maintained in parallel. The Chinese version uses a `.zh-CN` suffix (e.g., `spec/SPEC.zh-CN.md`), and the English version uses no suffix (e.g., `spec/SPEC.md`). English files may be AI-translated from the Chinese source — add the following notice at the top of any such file:
  ```
  > This document was translated from the Chinese source by AI. The Chinese version is the authoritative reference.
  ```
- **Git commit messages**: always in English.
- **Code and comments**: English preferred.

## Repository Structure

- `spec/SPEC.zh-CN.md`: TinyPath language specification (Chinese)
- `tests.json`: Compliance test cases (not yet created, but the format is defined in the spec)

## Language Spec Key Points

TinyPath expressions are composed of:

- **Root node**: `$` — all expressions must start with this
- **Field access**: `["fieldName"]` — field name in double quotes, must not be empty
- **Array index**: `[n]` — non-negative integers only
- **Array filter**: `[?(@["key"]==value)]` — supports `&&` combinations, returns the first matching element
- No whitespace is allowed anywhere in expression structure (except inside string values)
- No whitespace allowed on either side of `==` or `&&`

**Error handling principle**: syntax errors throw exceptions; data issues (path not found, out of bounds, no match) return `null`.

## Compliance Testing

All implementations must pass every test case in `tests.json`. Test case format:

```json
{
  "description": "test description",
  "data": {},
  "expression": "$[...]",
  "expected": "expected result or null"
}
```

When adding a new language implementation, read `tests.json` directly to drive tests and verify compliance.

## Unsupported Features

Explicitly not supported: wildcard `[*]`, recursive descent `..`, negative indices, array slices, `!=`/`>`/`<` comparisons, logical OR `||`, single quotes, dot notation (`$.field`), and structural whitespace.
