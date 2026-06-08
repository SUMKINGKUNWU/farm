import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  ApiClientError,
  createApiError,
  errorCodeActions,
  errorCodeMessages,
  normalizeError
} from './apiError.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const backendErrorCodePath = path.resolve(
  __dirname,
  '../../backend/src/main/java/com/farm/exchange/common/ErrorCode.java'
)

const backendErrorCodeSource = fs.readFileSync(backendErrorCodePath, 'utf8')

function extractBackendCodes(prefix) {
  const pattern = /public static final String ([A-Z_]+) = "[A-Z_]+";/g
  return Array.from(backendErrorCodeSource.matchAll(pattern), ([, code]) => code)
    .filter((code) => code.startsWith(prefix))
}

const balanceAndInventoryCodes = ['INSUFFICIENT_BALANCE', 'INSUFFICIENT_INVENTORY']
const stateConflictCodes = ['STATE_CONFLICT']
const bulkTokenCodes = extractBackendCodes('BULK_TOKEN_')

function buildResponse(status = 409) {
  return { status }
}

test('createApiError 对余额、库存和状态冲突错误保留后端 code 并补齐前端文案', () => {
  for (const code of [...balanceAndInventoryCodes, ...stateConflictCodes]) {
    const apiError = createApiError({ code, message: `backend:${code}` }, buildResponse())
    assert.ok(apiError instanceof ApiClientError)
    assert.equal(apiError.code, code)
    assert.equal(apiError.message, errorCodeMessages[code])
    assert.equal(apiError.action, errorCodeActions[code])
  }
})

test('前端覆盖后端全部大宗令牌错误码，并能稳定识别 message 与 action', () => {
  assert.deepEqual(bulkTokenCodes, [
    'BULK_TOKEN_REQUIRED',
    'BULK_TOKEN_INVALID',
    'BULK_TOKEN_EXPIRED',
    'BULK_TOKEN_LIMIT_EXCEEDED'
  ])

  for (const code of bulkTokenCodes) {
    assert.ok(errorCodeMessages[code], `${code} 缺少 message 映射`)
    assert.ok(errorCodeActions[code], `${code} 缺少 action 映射`)

    const apiError = createApiError({ code, message: `backend:${code}` }, buildResponse(403))
    assert.equal(apiError.code, code)
    assert.equal(apiError.message, errorCodeMessages[code])
    assert.equal(apiError.action, errorCodeActions[code])
    assert.equal(apiError.status, 403)
  }
})

test('未知错误码保留后端 message，避免前端把已返回的 code 吞掉', () => {
  const apiError = createApiError(
    { code: 'UNMAPPED_BACKEND_CODE', message: 'backend fallback message' },
    buildResponse(400)
  )

  assert.equal(apiError.code, 'UNMAPPED_BACKEND_CODE')
  assert.equal(apiError.message, 'backend fallback message')
  assert.equal(apiError.action, '')
})

test('normalizeError 保留 ApiClientError，普通 Error 转为 CLIENT_ERROR', () => {
  const existing = new ApiClientError({ code: 'STATE_CONFLICT', message: 'conflict' })
  assert.equal(normalizeError(existing), existing)

  const normalized = normalizeError(new Error('network down'))
  assert.equal(normalized.code, 'CLIENT_ERROR')
  assert.equal(normalized.message, 'network down')
})
