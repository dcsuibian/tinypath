> This document was translated from the Chinese source by AI. The Chinese version is the authoritative reference.

# tinypath-py

Python implementation of [TinyPath](https://github.com/dcsuibian/tinypath), a minimal JSON path expression language for IoT scenarios.

## Installation

```bash
pip install tinypath-py
```

## Usage

```python
from tinypath import TinyPath

path = TinyPath('$["sensors"][?(@["type"]=="temp"&&@["id"]=="01")]["readings"][0]["value"]')
value = path.evaluate(json_str)  # "36.500"
```

`TinyPath(expression)` compiles the expression and raises `TinyPathException` on syntax errors. `evaluate(json_str)` returns `None` when the path does not resolve.

## Running Tests

Tests are driven by the compliance test cases in `spec/tests.json` at the repository root.

```bash
uv run pytest
```

To point at a different test file:

```bash
TINYPATH_TESTS=/path/to/tests.json uv run pytest
```
