export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

export type Role = 'STUDENT' | 'MODERATOR' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'WARNED' | 'BANNED'
export type ConditionLevel = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR'
export type TradeMethod = 'MEETUP' | 'EXPRESS'
export type ItemStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'ON_SALE'
  | 'REJECTED'
  | 'LOCKED'
  | 'SOLD'
  | 'OFF_SHELF'
export type OrderStatus =
  | 'PENDING_PAY'
  | 'PAID'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDING'
  | 'REFUNDED'
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'
export type ReportTargetType = 'ITEM' | 'USER'
export type ReportReason = 'COUNTERFEIT' | 'SCAM' | 'ABUSE' | 'PROHIBITED' | 'OTHER'

export interface UserSummary {
  id: number
  studentNo: string
  nickname: string
  email: string
  avatarUrl?: string | null
  campus?: string | null
  role: Role
  verified: boolean
  creditScore?: number | null
  avgRating?: number | null
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserSummary
}

export interface Category {
  id: number
  name: string
  slug: string
  icon?: string | null
  sortOrder: number
  enabled: boolean
}

export interface ItemSummary {
  id: number
  title: string
  price: number
  originalPrice?: number | null
  coverUrl?: string | null
  conditionLevel: ConditionLevel
  tradeMethod: TradeMethod
  status: ItemStatus
  categoryId?: number | null
  categoryName?: string | null
  sellerId?: number | null
  sellerNickname?: string | null
  viewCount: number
  favoriteCount: number
  favorited: boolean
  createdAt: string
}

export interface ItemDetail {
  id: number
  title: string
  description: string
  price: number
  originalPrice?: number | null
  conditionLevel: ConditionLevel
  tradeMethod: TradeMethod
  status: ItemStatus
  rejectReason?: string | null
  tags?: string | null
  campus?: string | null
  categoryId?: number | null
  categoryName?: string | null
  sellerId: number
  sellerNickname?: string | null
  sellerAvatar?: string | null
  sellerVerified: boolean
  sellerCreditScore?: number | null
  viewCount: number
  favoriteCount: number
  favorited: boolean
  mine: boolean
  images: string[]
  publishedAt?: string | null
  createdAt: string
}

export interface ItemPayload {
  title: string
  description: string
  price: number | null
  originalPrice: number | null
  conditionLevel: ConditionLevel
  categoryId: number | null
  campus: string
  tradeMethod: TradeMethod
  tags: string
  imageUrls: string[]
  submitNow: boolean
}

export interface FavoriteToggle {
  favorited: boolean
  favoriteCount: number
}

export interface OrderActionFlags {
  pay: boolean
  cancel: boolean
  ship: boolean
  confirm: boolean
  refundRequest: boolean
  refundApprove: boolean
  refundReject: boolean
  review: boolean
}

export interface Order {
  id: number
  orderNo: string
  itemId: number
  itemTitle?: string | null
  itemCover?: string | null
  buyerId: number
  buyerNickname?: string | null
  sellerId: number
  sellerNickname?: string | null
  amount: number
  feeAmount?: number | null
  sellerIncome?: number | null
  status: OrderStatus
  tradeMethod: TradeMethod
  payDeadline: string
  paidAt?: string | null
  shippedAt?: string | null
  completedAt?: string | null
  cancelledAt?: string | null
  cancelReason?: string | null
  createdAt: string
  actions: OrderActionFlags
}

export interface OrderStatusLog {
  createdAt: string
  fromStatus: string
  toStatus: string
  remark?: string | null
  operatorNickname?: string | null
}

export interface PaymentRecord {
  id: number
  tradeNo: string
  type: string
  amount: number
  status: string
  createdAt: string
}

export interface OrderDetail {
  order: Order
  timeline: OrderStatusLog[]
  reviewedByMe: boolean
  platformFeeRate: number
  payments: PaymentRecord[]
}

export interface WalletInfo {
  id: number
  balance: number
  frozen: number
  totalIncome: number
  totalSpent: number
}

export interface LedgerEntry {
  id: number
  type: string
  amount: number
  balanceAfter: number
  orderId?: number | null
  remark?: string | null
  createdAt: string
}

export interface Conversation {
  id: number
  itemId: number
  itemTitle: string
  itemCover?: string | null
  counterpartId: number
  counterpartNickname: string
  lastMessage: string
  lastMessageAt?: string | null
  unreadCount: number
}

export interface ChatMessage {
  id: number
  conversationId: number
  senderId: number
  senderNickname: string
  type: 'TEXT' | 'IMAGE' | 'OFFER'
  content: string
  offerAmount?: number | null
  createdAt: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  link?: string | null
  read: boolean
  createdAt: string
}

export interface Reputation {
  creditScore: number
  badge: string
  badgeLabel: string
  totalDeals: number
  completedDeals: number
  cancelledDeals: number
  refundedDeals: number
  totalReviews: number
  goodReviews: number
  badReviews: number
  disputeCount: number
  reportCount: number
  avgRating: number
}

export interface Review {
  id: number
  orderId: number
  itemId: number
  itemTitle?: string | null
  rating: number
  tags?: string | null
  comment?: string | null
  direction: 'BUYER_TO_SELLER' | 'SELLER_TO_BUYER'
  reviewerId: number
  reviewerNickname?: string | null
  createdAt: string
}

export interface PublicProfile {
  user: UserSummary
  reputation: Reputation
  onSaleItems: number
  verified: boolean
  recentReviews: {
    id: number
    rating: number
    tags?: string | null
    comment?: string | null
    reviewerNickname?: string | null
    createdAt: string
  }[]
}

export interface ReportItem {
  id: number
  reporterId: number
  reporterNickname?: string | null
  targetType: ReportTargetType
  targetId: number
  targetTitle?: string | null
  reasonType: ReportReason
  reasonLabel: string
  description: string
  evidenceUrl?: string | null
  status: ReportStatus
  handlerId?: number | null
  handlerNickname?: string | null
  handleRemark?: string | null
  handledAt?: string | null
  createdAt: string
}

export interface AdminUser {
  id: number
  studentNo: string
  nickname: string
  email: string
  avatarUrl?: string | null
  campus?: string | null
  role: Role
  status: UserStatus
  verified: boolean
  creditScore?: number | null
  badge?: string | null
  onSaleItems: number
  bannedUntil?: string | null
  lastLoginAt?: string | null
  createdAt: string
}

export interface DashboardSummary {
  totalUsers: number
  newUsers7d: number
  onSaleItems: number
  pendingItems: number
  pendingReports: number
  totalOrders: number
  completedOrders: number
  gmv: number
  gmv7d: number
  todayOrders: number
  todayUsers: number
}

export interface TrendPoint {
  date: string
  orders: number
  gmv: number
  newUsers: number
  newItems: number
}

export interface CategoryStat {
  categoryId: number
  categoryName: string
  soldCount: number
  gmv: number
}

export interface AuditLogEntry {
  id: number
  actorId?: number | null
  actorNickname: string
  action: string
  targetType: string
  targetId?: number | null
  detail?: string | null
  ip?: string | null
  createdAt: string
}
