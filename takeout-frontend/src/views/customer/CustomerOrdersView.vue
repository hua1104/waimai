<template>
  <div class="page">
    <header class="topbar">
      <div class="title">我的订单（食客端）</div>
      <div class="actions">
        <RouterLink class="btn" to="/c">返回点餐</RouterLink>
        <RouterLink class="btn" to="/c/profile">个人信息</RouterLink>
        <RouterLink class="btn" to="/c/stats">我的统计</RouterLink>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card">
        <div class="filters">
          <label>
            状态
            <select v-model="filters.status">
              <option value="">全部</option>
              <option value="CREATED">CREATED</option>
              <option value="PAID">PAID</option>
              <option value="DELIVERING">DELIVERING</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="CANCELED">CANCELED</option>
            </select>
          </label>
          <button class="btn primary" @click="loadOrders(1)">查询</button>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>饭店</th>
              <th>金额</th>
              <th>状态</th>
              <th>支付</th>
              <th>创建时间</th>
              <th>支付时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in orders" :key="o.id">
              <td>{{ o.id }}</td>
              <td>{{ o.restaurantName }}</td>
              <td>{{ o.payAmount ?? 0 }}</td>
  <td>{{ o.status }}</td>
              <td>
                {{ o.payStatus }}
                <span v-if="o.payStatus === 'REFUNDED'" class="tag warn">退款</span>
              </td>
              <td>{{ formatTime(o.createdAt) }}</td>
              <td>{{ formatTime(o.paidAt) }}</td>
              <td class="actions-cell">
                <button class="btn" @click="openDetail(o.id)">详情</button>
                <button v-if="canCancel(o.status, o.payStatus)" class="btn danger" @click="cancelOrder(o.id)">
                  {{ cancelBtnText(o) }}
                </button>
              </td>
            </tr>
            <tr v-if="!loading && orders.length === 0">
              <td colspan="8" class="empty">暂无数据</td>
            </tr>
            <tr v-if="loading">
              <td colspan="8" class="empty">加载中...</td>
            </tr>
          </tbody>
        </table>

        <div class="pager">
          <button class="btn" :disabled="page <= 1" @click="loadOrders(page - 1)">上一页</button>
          <span>第 {{ page }} 页，共 {{ total }} 条</span>
          <button class="btn" :disabled="page * size >= total" @click="loadOrders(page + 1)">下一页</button>
        </div>

        <div v-if="detail" class="detail">
          <div class="detail-head">
            <h3>订单详情 #{{ detail.id }}</h3>
            <button class="btn" @click="detail = null">关闭</button>
          </div>

          <div class="grid">
            <div><b>饭店</b>：{{ detail.restaurantName }}</div>
            <div><b>状态</b>：{{ detail.status }}</div>
            <div>
              <b>支付状态</b>：{{ detail.payStatus }}
              <span v-if="detail.payStatus === 'REFUNDED'" class="tag warn">退款</span>
            </div>
            <div v-if="detail.refundedAt"><b>退款时间</b>：{{ formatTime(detail.refundedAt) }}</div>
            <div v-if="detail.cancelReason"><b>取消原因</b>：{{ detail.cancelReason }}</div>
            <div><b>地址</b>：{{ detail.addressDetail ?? '' }}</div>
            <div><b>联系人</b>：{{ detail.contactName }} {{ detail.contactPhone }}</div>
            <div v-if="detail.remark"><b>备注</b>：{{ detail.remark }}</div>
            <div v-if="detail.deliveryStaffName || detail.deliveryStaffPhone">
              <b>骑手</b>：{{ detail.deliveryStaffName }} {{ detail.deliveryStaffPhone }}
            </div>
          </div>

          <h4 class="sub">明细</h4>
          <table class="table">
            <thead>
              <tr>
                <th>菜品</th>
                <th>单价</th>
                <th>数量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in detail.items" :key="i.id">
                <td>{{ i.dishName }}</td>
                <td>{{ i.unitPrice ?? 0 }}</td>
                <td>{{ i.quantity }}</td>
              </tr>
              <tr v-if="detail.items.length === 0">
                <td colspan="3" class="empty">无明细</td>
              </tr>
            </tbody>
          </table>

          <div v-if="detail.status === 'COMPLETED'" class="rate">
            <h4 class="sub">评价</h4>
            <div v-if="ratingLoading" class="empty">加载评价状态...</div>
              <div v-else class="rate-grid">
              <div class="card rate-card">
                <div class="rate-title">
                  <b>饭店评价</b>
                  <span v-if="ratingStatus?.restaurantRated" class="tag ok">已评价</span>
                  <span v-else class="tag">未评价</span>
                </div>
                <div class="rate-row">
                  <label>
                    评分
                    <select v-model.number="restaurantRating.score" :disabled="ratingStatus?.restaurantRated">
                      <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
                    </select>
                  </label>
                  <label class="grow">
                    内容
                    <textarea
                      v-model="restaurantRating.content"
                      :disabled="ratingStatus?.restaurantRated"
                      placeholder="可选：写下你的体验（最多500字）"
                    />
                  </label>
                  <button
                    class="btn primary"
                    :disabled="ratingStatus?.restaurantRated"
                    @click="submitRestaurantRating"
                  >
                    提交
                  </button>
                </div>
              </div>

              <div class="card rate-card">
                <div class="rate-title">
                  <b>骑手评价</b>
                  <span v-if="ratingStatus?.hasDeliveryStaff === false" class="tag warn">无骑手</span>
                  <span v-else-if="ratingStatus?.deliveryRated" class="tag ok">已评价</span>
                  <span v-else class="tag">未评价</span>
                </div>
                <div class="rate-row">
                  <label>
                    评分
                    <select
                      v-model.number="deliveryRating.score"
                      :disabled="ratingStatus?.deliveryRated || ratingStatus?.hasDeliveryStaff === false"
                    >
                      <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
                    </select>
                  </label>
                  <label class="grow">
                    内容
                    <textarea
                      v-model="deliveryRating.content"
                      :disabled="ratingStatus?.deliveryRated || ratingStatus?.hasDeliveryStaff === false"
                      placeholder="可选：写下你的体验（最多500字）"
                    />
                  </label>
                  <button
                    class="btn primary"
                    :disabled="ratingStatus?.deliveryRated || ratingStatus?.hasDeliveryStaff === false"
                    @click="submitDeliveryRating"
                  >
                    提交
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="msg" class="msg">{{ msg }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../lib/api'

interface OrderListItem {
  id: number
  restaurantId: number
  restaurantName: string
  status: string
  payStatus: string
  payAmount: number
  createdAt: string
  paidAt: string | null
}

interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

interface OrderItemRow {
  id: number
  dishId: number | null
  dishName: string
  unitPrice: number
  quantity: number
}

interface OrderDetail {
  id: number
  restaurantId: number
  restaurantName: string
  status: string
  payStatus: string
  totalAmount: number
  payAmount: number
  createdAt: string
  paidAt: string | null
  finishedAt: string | null
  refundedAt: string | null
  cancelReason: string | null
  addressDetail: string | null
  contactName: string | null
  contactPhone: string | null
  remark: string | null
  deliveryStaffId: number | null
  deliveryStaffName: string | null
  deliveryStaffPhone: string | null
  items: OrderItemRow[]
}

const router = useRouter()

const loading = ref(false)
const orders = ref<OrderListItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const msg = ref('')

const filters = ref({
  status: '',
})

const detail = ref<OrderDetail | null>(null)
const ratingStatus = ref<{ restaurantRated: boolean; deliveryRated: boolean; hasDeliveryStaff: boolean } | null>(null)
const ratingLoading = ref(false)

const restaurantRating = ref({ score: 5, content: '' })
const deliveryRating = ref({ score: 5, content: '' })

const customerId = computed<number | null>(() => {
  try {
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const v = user?.userId
    return typeof v === 'number' ? v : v ? Number(v) : null
  } catch {
    return null
  }
})

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

function canCancel(status: string, payStatus: string) {
  if (status === 'CREATED') return true
  if (status === 'PAID' && payStatus === 'PAID') return true
  return false
}

function cancelBtnText(o: OrderListItem) {
  if (o.status === 'PAID' && o.payStatus === 'PAID') return '取消并退款'
  return '取消'
}

async function loadOrders(targetPage: number) {
  if (!customerId.value) return
  loading.value = true
  msg.value = ''
  try {
    const qs = new URLSearchParams()
    qs.set('customerId', String(customerId.value))
    qs.set('page', String(targetPage))
    qs.set('size', String(size.value))
    if (filters.value.status) qs.set('status', filters.value.status)

    const res = await fetch(api(`/api/customer/orders?${qs.toString()}`))
    if (!res.ok) return
    const data = (await res.json()) as PageResponse<OrderListItem>
    orders.value = data.items
    page.value = data.page
    size.value = data.size
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  if (!customerId.value) return
  const res = await fetch(api(`/api/customer/orders/${id}?customerId=${customerId.value}`))
  if (!res.ok) return
  detail.value = (await res.json()) as OrderDetail
  restaurantRating.value = { score: 5, content: '' }
  deliveryRating.value = { score: 5, content: '' }
  await loadRatingStatus(detail.value.id)
}

async function loadRatingStatus(orderId: number) {
  if (!customerId.value) return
  ratingLoading.value = true
  try {
    const res = await fetch(api(`/api/customer/ratings/order/${orderId}?customerId=${customerId.value}`))
    if (!res.ok) {
      ratingStatus.value = null
      return
    }
    ratingStatus.value = await res.json()
  } finally {
    ratingLoading.value = false
  }
}

async function submitRestaurantRating() {
  if (!customerId.value || !detail.value) return
  if (ratingStatus.value?.restaurantRated) return
  msg.value = ''
  const res = await fetch(api('/api/customer/ratings/restaurant'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      customerId: customerId.value,
      orderId: detail.value.id,
      score: restaurantRating.value.score,
      content: restaurantRating.value.content,
    }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '提交饭店评价失败'
    return
  }
  msg.value = '饭店评价已提交'
  await loadRatingStatus(detail.value.id)
}

async function submitDeliveryRating() {
  if (!customerId.value || !detail.value) return
  if (ratingStatus.value?.deliveryRated || ratingStatus.value?.hasDeliveryStaff === false) return
  msg.value = ''
  const res = await fetch(api('/api/customer/ratings/delivery'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      customerId: customerId.value,
      orderId: detail.value.id,
      score: deliveryRating.value.score,
      content: deliveryRating.value.content,
    }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '提交骑手评价失败'
    return
  }
  msg.value = '骑手评价已提交'
  await loadRatingStatus(detail.value.id)
}

async function cancelOrder(id: number) {
  if (!customerId.value) return
  msg.value = ''
  const reason = prompt('请输入取消原因（可选）：', '')
  if (reason === null) return
  if (!confirm('确认取消该订单吗？（已支付订单将自动退款）')) return
  const res = await fetch(api(`/api/customer/orders/${id}/cancel`), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerId: customerId.value, reason }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '取消失败'
    return
  }
  msg.value = '操作成功'
  if (detail.value?.id === id) detail.value = null
  await loadOrders(page.value)
}

onMounted(() => loadOrders(1))
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--bg);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.78);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  gap: 12px;
}

.title {
  font-weight: 900;
  letter-spacing: 0.5px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.content {
  padding: 18px 16px 28px;
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  padding: 16px;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: end;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--muted);
}

.actions-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty {
  text-align: center;
  color: var(--muted);
}

.pager {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.detail {
  margin-top: 16px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding-top: 16px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(200px, 1fr));
  gap: 8px 12px;
  font-size: 13px;
}

.grid b {
  color: rgba(15, 23, 42, 0.82);
}

@media (max-width: 920px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

.sub {
  margin: 10px 0 6px;
}

.rate {
  margin-top: 12px;
}

.rate-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.rate-card {
  padding: 12px;
}

.rate-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.rate-row {
  display: flex;
  gap: 10px;
  align-items: end;
  flex-wrap: wrap;
}

.rate-row .grow {
  flex: 1;
  min-width: 260px;
}

textarea {
  min-height: 72px;
  resize: vertical;
}

.msg {
  margin-top: 10px;
  color: rgb(21, 128, 61);
  font-size: 13px;
}
</style>
