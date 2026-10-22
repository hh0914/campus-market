import type { ConditionLevel, ItemStatus, OrderStatus, ReportReason, ReportStatus, TradeMethod } from '@/types'

export const CONDITION_LABELS: Record<ConditionLevel, string> = {
  NEW: '全新',
  LIKE_NEW: '几乎全新',
  GOOD: '轻微使用痕迹',
  FAIR: '明显使用痕迹',
}

export const ITEM_STATUS_LABELS: Record<ItemStatus, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  ON_SALE: '在售',
  REJECTED: '已驳回',
  LOCKED: '交易中',
  SOLD: '已售出',
  OFF_SHELF: '已下架',
}

export const ITEM_STATUS_TONE: Record<ItemStatus, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  ON_SALE: 'success',
  REJECTED: 'danger',
  LOCKED: 'primary',
  SOLD: 'info',
  OFF_SHELF: 'info',
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAY: '待付款',
  PAID: '待发货',
  SHIPPED: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}

export const ORDER_STATUS_TONE: Record<OrderStatus, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  PENDING_PAY: 'warning',
  PAID: 'primary',
  SHIPPED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  REFUNDING: 'danger',
  REFUNDED: 'danger',
}

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已成立',
  REJECTED: '已驳回',
}

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  COUNTERFEIT: '假冒伪劣',
  SCAM: '欺诈行为',
  ABUSE: '辱骂骚扰',
  PROHIBITED: '违禁物品',
  OTHER: '其他',
}

export const TRADE_METHOD_LABELS: Record<TradeMethod, string> = {
  MEETUP: '当面交易',
  EXPRESS: '快递寄送',
}

export const LEDGER_TYPE_LABELS: Record<string, string> = {
  RECHARGE: '充值',
  ESCROW_OUT: '托管付款',
  ESCROW_IN: '托管结算',
  REFUND: '退款',
  INCOME: '交易收入',
  FEE: '平台手续费',
}

export const ROLE_LABELS: Record<string, string> = {
  STUDENT: '学生',
  MODERATOR: '审核员',
  ADMIN: '管理员',
}

export const USER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '正常',
  WARNED: '已警告',
  BANNED: '已封禁',
}

export function formatMoney(value?: number | string | null): string {
  const amount = Number(value ?? 0)
  if (Number.isNaN(amount)) {
    return '¥0.00'
  }
  return `¥${amount.toFixed(2)}`
}

export function formatNumber(value?: number | null): string {
  return new Intl.NumberFormat('zh-CN').format(Number(value ?? 0))
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

export function formatDate(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN')
}

export function relativeTime(value?: string | null): string {
  if (!value) {
    return ''
  }
  const date = new Date(value).getTime()
  if (Number.isNaN(date)) {
    return ''
  }
  const diff = Date.now() - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) {
    return '刚刚'
  }
  if (diff < hour) {
    return `${Math.floor(diff / minute)} 分钟前`
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`
  }
  if (diff < 30 * day) {
    return `${Math.floor(diff / day)} 天前`
  }
  return formatDate(value)
}

export function splitTags(tags?: string | null): string[] {
  if (!tags) {
    return []
  }
  return tags
    .split(/[,，\s]+/)
    .map((tag) => tag.trim())
    .filter(Boolean)
}
