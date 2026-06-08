export const errorCodeMessages = {
  AUTH_REQUIRED: '请先登录后再操作。',
  AUTH_INVALID: '登录凭证无效，请重新登录。',
  AUTH_EXPIRED: '登录已过期，请重新登录。',
  AUTH_FAILED: '用户名或密码错误。',
  ADMIN_REQUIRED: '该账号不是管理员。',
  PERMISSION_DENIED: '当前账号没有执行该操作的权限。',
  USER_NOT_FOUND: '用户不存在或状态不可用。',
  USER_INACTIVE: '用户状态不可用。',
  USERNAME_EXISTS: '用户名已存在。',
  TRADE_PASSWORD_REQUIRED: '请先设置交易密码。',
  TRADE_PASSWORD_LOCKED: '交易密码已锁定，请稍后再试。',
  TRADE_PASSWORD_INVALID: '交易密码错误。',
  INSUFFICIENT_BALANCE: '金币余额不足。',
  INSUFFICIENT_INVENTORY: '库存不足，无法完成操作。',
  BULK_TOKEN_REQUIRED: '该交易需要大宗交易令牌。',
  BULK_TOKEN_INVALID: '大宗交易令牌无效或不可用。',
  BULK_TOKEN_EXPIRED: '大宗交易令牌已过期。',
  BULK_TOKEN_LIMIT_EXCEEDED: '交易金额超过大宗令牌限制。',
  RESOURCE_NOT_FOUND: '目标资源不存在。',
  ITEM_NOT_FOUND: '商品不存在或不可用。',
  ITEM_NOT_TRADABLE: '该商品当前不可交易。',
  WALLET_NOT_FOUND: '钱包不存在。',
  STATE_CONFLICT: '数据状态已变化，请刷新后重试。',
  CONFIG_MISSING: '系统配置缺失，请联系管理员。',
  INVALID_OPERATION: '当前操作不被支持。',
  VALIDATION_ERROR: '请检查表单填写内容。',
  TARGET_USER_REQUIRED: '请先填写目标玩家用户 ID。',
  MISSING_PARAMETER: '请求参数缺失。',
  TYPE_MISMATCH: '参数格式不正确。',
  BAD_REQUEST: '请求内容格式不正确。',
  METHOD_NOT_ALLOWED: '请求方式不支持。',
  DATA_CONFLICT: '数据冲突，请刷新后重试。',
  DATABASE_ERROR: '数据库处理失败，请稍后重试。',
  INTERNAL_ERROR: '系统异常，请稍后重试。'
}

export const errorCodeActions = {
  AUTH_REQUIRED: '重新登录管理台后再继续。',
  AUTH_INVALID: '退出登录并重新获取 token。',
  AUTH_EXPIRED: '退出登录并重新获取 token。',
  PERMISSION_DENIED: '确认当前账号是否已经被设置为 ADMIN。',
  ADMIN_REQUIRED: '请切换为管理员账号后重新登录管理台。',
  TRADE_PASSWORD_REQUIRED: '玩家侧先完成交易密码设置。',
  TRADE_PASSWORD_LOCKED: '等待锁定时间结束后再尝试。',
  INSUFFICIENT_BALANCE: '降低交易数量或先补充金币。',
  INSUFFICIENT_INVENTORY: '确认玩家库存、锁定库存和交易数量。',
  BULK_TOKEN_REQUIRED: '先由管理员发放符合条件的大宗交易令牌。',
  BULK_TOKEN_INVALID: '检查令牌归属、商品类型和状态。',
  BULK_TOKEN_EXPIRED: '重新发放有效期内的大宗令牌。',
  BULK_TOKEN_LIMIT_EXCEEDED: '提高令牌限额或降低单笔交易金额。',
  ITEM_NOT_TRADABLE: '检查商品类型、状态和 trade_enabled 配置。',
  STATE_CONFLICT: '刷新当前数据后重新提交。',
  CONFIG_MISSING: '检查税率、扩建或商品生产配置。',
  VALIDATION_ERROR: '根据字段提示修正后重新提交。',
  TARGET_USER_REQUIRED: '在左侧身份卡填写目标玩家用户 ID 后再刷新资产、交易或发放令牌。'
}

export class ApiClientError extends Error {
  constructor({ message, code, status, path, timestamp, fieldErrors, raw }) {
    super(message)
    this.name = 'ApiClientError'
    this.code = code || 'REQUEST_FAILED'
    this.status = status || 0
    this.path = path || ''
    this.timestamp = timestamp || ''
    this.fieldErrors = Array.isArray(fieldErrors) ? fieldErrors : []
    this.raw = raw || null
    this.action = errorCodeActions[this.code] || ''
  }
}

export function normalizeError(error) {
  if (error instanceof ApiClientError) return error
  return new ApiClientError({
    message: error instanceof Error ? error.message : String(error),
    code: 'CLIENT_ERROR'
  })
}

export function createApiError(body, response) {
  const code = body?.code || `HTTP_${response.status}`
  return new ApiClientError({
    code,
    status: body?.status || response.status,
    message: errorCodeMessages[code] || body?.message || `请求失败：${response.status}`,
    path: body?.path || '',
    timestamp: body?.timestamp || '',
    fieldErrors: body?.fieldErrors || [],
    raw: body
  })
}
