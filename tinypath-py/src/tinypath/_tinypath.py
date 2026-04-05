import json
from dataclasses import dataclass
from typing import Any


class TinyPathException(Exception):
    """Raised when a TinyPath expression contains a syntax error or the input JSON is invalid."""
    pass


@dataclass(frozen=True)
class _FieldStep:
    name: str


@dataclass(frozen=True)
class _IndexStep:
    index: int


@dataclass(frozen=True)
class _Condition:
    key: str
    value: Any


@dataclass(frozen=True)
class _FilterStep:
    conditions: tuple[_Condition, ...]


_Step = _FieldStep | _IndexStep | _FilterStep

_MISSING = object()


class _Parser:
    def __init__(self, expression: str) -> None:
        self._expr = expression
        self._pos = 0

    def peek(self) -> str | None:
        return self._expr[self._pos] if self._pos < len(self._expr) else None

    def consume(self) -> str:
        ch = self._expr[self._pos]
        self._pos += 1
        return ch

    def expect(self, ch: str) -> None:
        if self._pos >= len(self._expr) or self._expr[self._pos] != ch:
            got = self._expr[self._pos] if self._pos < len(self._expr) else "end of expression"
            raise TinyPathException(
                f"Expected '{ch}' at position {self._pos}, got '{got}': {self._expr}"
            )
        self._pos += 1

    def parse_quoted_string(self) -> str:
        self.expect('"')
        result = []
        while True:
            if self._pos >= len(self._expr):
                raise TinyPathException(f"Unterminated string in expression: {self._expr}")
            ch = self.consume()
            if ch == '"':
                return ''.join(result)
            if ch == '\\':
                if self._pos >= len(self._expr):
                    raise TinyPathException(f"Unterminated escape in expression: {self._expr}")
                escaped = self.consume()
                match escaped:
                    case '"':  result.append('"')
                    case '\\': result.append('\\')
                    case '/':  result.append('/')
                    case 'b':  result.append('\b')
                    case 'f':  result.append('\f')
                    case 'n':  result.append('\n')
                    case 'r':  result.append('\r')
                    case 't':  result.append('\t')
                    case _: raise TinyPathException(
                        f"Invalid escape '\\{escaped}' in expression: {self._expr}"
                    )
            else:
                result.append(ch)

    def parse_number(self) -> int | float:
        start = self._pos
        while self._pos < len(self._expr) and self._expr[self._pos].isdigit():
            self._pos += 1
        if self._pos < len(self._expr) and self._expr[self._pos] == '.':
            self._pos += 1
            while self._pos < len(self._expr) and self._expr[self._pos].isdigit():
                self._pos += 1
            return float(self._expr[start:self._pos])
        return int(self._expr[start:self._pos])

    def parse_filter_value(self) -> Any:
        if self._expr[self._pos:self._pos + 4] == 'null':
            self._pos += 4
            return None
        if self._expr[self._pos:self._pos + 4] == 'true':
            self._pos += 4
            return True
        if self._expr[self._pos:self._pos + 5] == 'false':
            self._pos += 5
            return False
        if self.peek() == '"':
            return self.parse_quoted_string()
        if self.peek() is None or not self._expr[self._pos].isdigit():
            got = self._expr[self._pos] if self._pos < len(self._expr) else "end of expression"
            raise TinyPathException(
                f"Unexpected character '{got}' in filter value at position {self._pos}: {self._expr}"
            )
        return self.parse_number()

    def parse_filter_step(self) -> _FilterStep:
        conditions = []
        while True:
            self.expect('@')
            self.expect('[')
            key = self.parse_quoted_string()
            self.expect(']')
            self.expect('=')
            self.expect('=')
            value = self.parse_filter_value()
            conditions.append(_Condition(key=key, value=value))
            if self.peek() == '&':
                self.expect('&')
                self.expect('&')
            else:
                break
        return _FilterStep(conditions=tuple(conditions))

    def parse(self) -> list[_Step]:
        if not self._expr.startswith('$'):
            raise TinyPathException(f"Expression must start with '$': {self._expr}")
        self._pos = 1

        steps: list[_Step] = []

        while self._pos < len(self._expr):
            self.expect('[')
            ch = self.peek()
            if ch is None:
                raise TinyPathException(f"Truncated expression: {self._expr}")
            if ch == '"':
                name = self.parse_quoted_string()
                self.expect(']')
                steps.append(_FieldStep(name=name))
            elif ch == '?':
                self.consume()
                self.expect('(')
                step = self.parse_filter_step()
                self.expect(')')
                self.expect(']')
                steps.append(step)
            elif ch == '*':
                raise TinyPathException(f"Wildcard is not supported: {self._expr}")
            elif ch == '.':
                raise TinyPathException(f"Recursive descent is not supported: {self._expr}")
            elif ch == "'":
                raise TinyPathException(f"Single quotes are not supported: {self._expr}")
            elif ch == '-':
                raise TinyPathException(f"Negative index is not supported: {self._expr}")
            elif ch.isdigit():
                index = self.parse_number()
                self.expect(']')
                steps.append(_IndexStep(index=int(index)))
            else:
                raise TinyPathException(
                    f"Unexpected character '{ch}' at position {self._pos}: {self._expr}"
                )

        return steps


def _evaluate(steps: list[_Step], data: Any) -> Any:
    current = data
    for step in steps:
        if current is None:
            return None
        if isinstance(step, _FieldStep):
            if not isinstance(current, dict):
                return None
            current = current.get(step.name)
        elif isinstance(step, _IndexStep):
            if not isinstance(current, list):
                return None
            if step.index >= len(current):
                return None
            current = current[step.index]
        elif isinstance(step, _FilterStep):
            if not isinstance(current, list):
                return None
            current = next(
                (
                    el for el in current
                    if isinstance(el, dict) and all(
                        _matches(el, c) for c in step.conditions
                    )
                ),
                None,
            )
    return current


def _matches(element: dict, condition: _Condition) -> bool:
    actual = element.get(condition.key, _MISSING)
    if actual is _MISSING:
        return condition.value is None
    return actual == condition.value


class TinyPath:
    """A compiled TinyPath expression that can be evaluated against JSON data.

    TinyPath is a minimal JSON path expression language designed for IoT scenarios.
    Expressions are compiled once and can be evaluated repeatedly against different inputs.

    Example::

        path = TinyPath('$["sensors"][0]["value"]')
        value = path.evaluate(json_str)
    """

    def __init__(self, expression: str) -> None:
        """Compile a TinyPath expression.

        Args:
            expression: The TinyPath expression string.

        Raises:
            TinyPathException: If the expression contains a syntax error.
        """
        self._expression = expression
        self._steps = _Parser(expression).parse()

    def evaluate(self, json_str: str) -> Any:
        """Evaluate this expression against the given JSON string.

        Returns ``None`` when the path does not resolve — for example, when a field
        is missing, an array index is out of bounds, or a filter has no match.

        If you need to evaluate multiple expressions against the same JSON data, prefer
        :meth:`evaluate_data` with pre-parsed data to avoid parsing the JSON string repeatedly.

        Args:
            json_str: The JSON string to evaluate against.

        Returns:
            The value at the path, or ``None`` if the path does not resolve.

        Raises:
            TinyPathException: If the JSON string is invalid.
        """
        try:
            data = json.loads(json_str)
        except json.JSONDecodeError as e:
            raise TinyPathException(f"Invalid JSON: {e}") from e
        return self.evaluate_data(data)

    def evaluate_data(self, data: Any) -> Any:
        """Evaluate this expression against already-parsed data.

        Use this method when evaluating multiple expressions against the same JSON data,
        so the JSON string is parsed only once via :func:`json.loads`.

        Returns ``None`` when the path does not resolve — for example, when a field
        is missing, an array index is out of bounds, or a filter has no match.

        Args:
            data: The parsed JSON data (typically a ``dict`` or ``list``).

        Returns:
            The value at the path, or ``None`` if the path does not resolve.
        """
        return _evaluate(self._steps, data)

    def __str__(self) -> str:
        return self._expression

    def __repr__(self) -> str:
        return f"TinyPath({self._expression!r})"
