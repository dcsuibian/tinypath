import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { describe, it, expect } from 'vitest'
import { TinyPath, TinyPathException } from '../src/index.js'

const dir = dirname(fileURLToPath(import.meta.url))
const testsPath = process.env['TINYPATH_TESTS'] ?? resolve(dir, '../../spec/tests.json')

interface TestCase {
  description: string
  expression: string
  data?: unknown
  expected?: unknown
  invalid?: boolean
}

const cases: TestCase[] = JSON.parse(readFileSync(testsPath, 'utf-8'))

describe('compliance', () => {
  for (const tc of cases) {
    it(tc.description, () => {
      if (tc.invalid) {
        expect(() => new TinyPath(tc.expression)).toThrow(TinyPathException)
        return
      }
      const result = new TinyPath(tc.expression).evaluate(JSON.stringify(tc.data))
      expect(result).toEqual(tc.expected)
    })
  }
})
