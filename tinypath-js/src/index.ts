export class TinyPathException extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'TinyPathException'
  }
}

type FieldStep = { kind: 'field'; name: string }
type IndexStep = { kind: 'index'; index: number }
type Condition = { key: string; value: unknown }
type FilterStep = { kind: 'filter'; conditions: Condition[] }
type Step = FieldStep | IndexStep | FilterStep

class Parser {
  private pos = 0

  constructor(private readonly expr: string) {}

  private peek(): string | undefined {
    return this.expr[this.pos]
  }

  private consume(): string {
    const ch = this.expr[this.pos]
    if (ch === undefined) {
      throw new TinyPathException(`Unexpected end of expression: ${this.expr}`)
    }
    this.pos++
    return ch
  }

  private expect(ch: string): void {
    if (this.expr[this.pos] !== ch) {
      const got = this.pos < this.expr.length ? this.expr[this.pos] : 'end of expression'
      throw new TinyPathException(`Expected '${ch}' at position ${this.pos}, got '${got}': ${this.expr}`)
    }
    this.pos++
  }

  private parseQuotedString(): string {
    this.expect('"')
    const result: string[] = []
    while (true) {
      if (this.pos >= this.expr.length) {
        throw new TinyPathException(`Unterminated string in expression: ${this.expr}`)
      }
      const ch = this.consume()
      if (ch === '"') {
        return result.join('')
      }
      if (ch === '\\') {
        if (this.pos >= this.expr.length) {
          throw new TinyPathException(`Unterminated escape in expression: ${this.expr}`)
        }
        const escaped = this.consume()
        switch (escaped) {
          case '"':
            result.push('"')
            break
          case '\\':
            result.push('\\')
            break
          case '/':
            result.push('/')
            break
          case 'b':
            result.push('\b')
            break
          case 'f':
            result.push('\f')
            break
          case 'n':
            result.push('\n')
            break
          case 'r':
            result.push('\r')
            break
          case 't':
            result.push('\t')
            break
          default:
            throw new TinyPathException(`Invalid escape '\\${escaped}' in expression: ${this.expr}`)
        }
      } else {
        result.push(ch)
      }
    }
  }

  private parseNumber(): number {
    const start = this.pos
    while (this.pos < this.expr.length && /\d/.test(this.expr[this.pos]!)) {
      this.pos++
    }
    if (this.pos < this.expr.length && this.expr[this.pos] === '.') {
      this.pos++
      while (this.pos < this.expr.length && /\d/.test(this.expr[this.pos]!)) {
        this.pos++
      }
      return parseFloat(this.expr.slice(start, this.pos))
    }
    return parseInt(this.expr.slice(start, this.pos), 10)
  }

  private parseFilterValue(): unknown {
    if (this.expr.startsWith('null', this.pos)) {
      this.pos += 4
      return null
    }
    if (this.expr.startsWith('true', this.pos)) {
      this.pos += 4
      return true
    }
    if (this.expr.startsWith('false', this.pos)) {
      this.pos += 5
      return false
    }
    if (this.peek() === '"') {
      return this.parseQuotedString()
    }
    const ch = this.peek()
    if (ch === undefined || !/\d/.test(ch)) {
      const got = ch ?? 'end of expression'
      throw new TinyPathException(`Unexpected character '${got}' in filter value at position ${this.pos}: ${this.expr}`)
    }
    return this.parseNumber()
  }

  private parseFilterStep(): FilterStep {
    const conditions: Condition[] = []
    while (true) {
      this.expect('@')
      this.expect('[')
      const key = this.parseQuotedString()
      this.expect(']')
      this.expect('=')
      this.expect('=')
      const value = this.parseFilterValue()
      conditions.push({ key, value })
      if (this.peek() === '&') {
        this.expect('&')
        this.expect('&')
      } else {
        break
      }
    }
    return { kind: 'filter', conditions }
  }

  parse(): Step[] {
    if (!this.expr.startsWith('$')) {
      throw new TinyPathException(`Expression must start with '$': ${this.expr}`)
    }
    this.pos = 1

    const steps: Step[] = []

    while (this.pos < this.expr.length) {
      this.expect('[')
      const ch = this.peek()
      if (ch === undefined) {
        throw new TinyPathException(`Truncated expression: ${this.expr}`)
      }
      if (ch === '"') {
        const name = this.parseQuotedString()
        this.expect(']')
        steps.push({ kind: 'field', name })
      } else if (ch === '?') {
        this.consume()
        this.expect('(')
        const step = this.parseFilterStep()
        this.expect(')')
        this.expect(']')
        steps.push(step)
      } else if (ch === '*') {
        throw new TinyPathException(`Wildcard is not supported: ${this.expr}`)
      } else if (ch === '.') {
        throw new TinyPathException(`Recursive descent is not supported: ${this.expr}`)
      } else if (ch === "'") {
        throw new TinyPathException(`Single quotes are not supported: ${this.expr}`)
      } else if (ch === '-') {
        throw new TinyPathException(`Negative index is not supported: ${this.expr}`)
      } else if (/\d/.test(ch)) {
        const index = this.parseNumber()
        this.expect(']')
        steps.push({ kind: 'index', index })
      } else {
        throw new TinyPathException(`Unexpected character '${ch}' at position ${this.pos}: ${this.expr}`)
      }
    }

    return steps
  }
}

function evaluate(steps: Step[], data: unknown): unknown {
  let current: unknown = data
  for (const step of steps) {
    if (current === null || current === undefined) {
      return null
    }
    if (step.kind === 'field') {
      if (typeof current !== 'object' || Array.isArray(current)) {
        return null
      }
      current = (current as Record<string, unknown>)[step.name] ?? null
    } else if (step.kind === 'index') {
      if (!Array.isArray(current)) {
        return null
      }
      if (step.index >= current.length) {
        return null
      }
      current = current[step.index] ?? null
    } else {
      if (!Array.isArray(current)) {
        return null
      }
      current =
        current.find(
          el =>
            typeof el === 'object' &&
            el !== null &&
            !Array.isArray(el) &&
            step.conditions.every(c => {
              const actual = (el as Record<string, unknown>)[c.key]
              return actual === c.value
            }),
        ) ?? null
    }
  }
  return current
}

/**
 * A compiled TinyPath expression that can be evaluated against JSON data.
 *
 * TinyPath is a minimal JSON path expression language designed for IoT scenarios.
 * Expressions are compiled once and can be evaluated repeatedly against different inputs.
 *
 * @example
 * ```ts
 * const path = new TinyPath('$["sensors"][0]["value"]');
 * const value = path.evaluate(jsonStr);
 * ```
 */
export class TinyPath {
  private readonly expression: string
  private readonly steps: Step[]

  constructor(expression: string) {
    this.expression = expression
    this.steps = new Parser(expression).parse()
  }

  /**
   * Evaluates this expression against the given JSON string.
   *
   * Returns `null` when the path does not resolve — for example, when a field
   * is missing, an array index is out of bounds, or a filter has no match.
   *
   * If you need to evaluate multiple expressions against the same JSON data, prefer
   * {@link evaluateData} with pre-parsed data to avoid parsing the JSON string repeatedly.
   *
   * @throws {TinyPathException} If the JSON string is invalid.
   */
  evaluate(jsonStr: string): unknown {
    let data: unknown
    try {
      data = JSON.parse(jsonStr) as unknown
    } catch (e) {
      throw new TinyPathException(`Invalid JSON: ${String(e)}`)
    }
    return this.evaluateData(data)
  }

  /**
   * Evaluates this expression against already-parsed data.
   *
   * Use this method when evaluating multiple expressions against the same JSON data,
   * so the JSON string is parsed only once via `JSON.parse`.
   *
   * Returns `null` when the path does not resolve — for example, when a field
   * is missing, an array index is out of bounds, or a filter has no match.
   */
  evaluateData(data: unknown): unknown {
    return evaluate(this.steps, data)
  }

  toString(): string {
    return this.expression
  }
}
