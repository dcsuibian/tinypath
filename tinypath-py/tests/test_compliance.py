import json
import os
from pathlib import Path

import pytest

from tinypath import TinyPath, TinyPathException

TESTS_JSON = Path(os.environ.get("TINYPATH_TESTS", Path(__file__).parent.parent.parent / "spec" / "tests.json"))


def load_cases():
    with open(TESTS_JSON, encoding="utf-8") as f:
        return json.load(f)


def pytest_generate_tests(metafunc):
    if "case" in metafunc.fixturenames:
        cases = load_cases()
        ids = [c["description"] for c in cases]
        metafunc.parametrize("case", cases, ids=ids)


def test_compliance(case):
    expression = case["expression"]

    if case.get("invalid"):
        print(f"[invalid] {case['description']}")
        with pytest.raises(TinyPathException):
            TinyPath(expression)
        return

    print(f"[valid] {case['description']}")
    path = TinyPath(expression)
    result = path.evaluate(json.dumps(case["data"]))
    assert result == case["expected"]
