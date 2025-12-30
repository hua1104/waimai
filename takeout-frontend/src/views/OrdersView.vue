<template>
  <div class="card">
    <h2>订单管理</h2>

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
      <label>
        支付状态
        <select v-model="filters.payStatus">
          <option value="">全部</option>
          <option value="UNPAID">UNPAID</option>
          <option value="PAID">PAID</option>
          <option value="REFUNDED">REFUNDED</option>
        </select>
      </label>
      <label>
        开始日期
        <input v-model="filters.start" type="date" />
      </label>
      <label>
        结束日期
        <input v-model="filters.end" type="date" />
      </label>
      <button @click="loadOrders(1)">查询</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>饭店</th>
          <th>食客</th>
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
          <td>{{ o.customerUsername }}</td>
          <td>{{ o.payAmount ?? 0 }}</td>
          <td>{{ o.status }}</td>
          <td>
            {{ o.payStatus }}
            <span v-if="o.payStatus === 'REFUNDED'" class="tag warn">退款</span>
          </td>
          <td>{{ formatTime(o.createdAt) }}</td>
          <td>{{ formatTime(o.paidAt) }}</td>
          <td class="actions">
            <button class="mini" @click="openDetail(o.id)">详情</button>
            <button
              v-if="role === 'RESTAURANT' && o.status === 'PAID'"
              class="mini primary"
              @click="confirmAndUpdate(o.id, 'DELIVERING', '接单')"
            >
              接单
            </button>
            <button
              v-if="role === 'RESTAURANT' && o.status === 'DELIVERING'"
              class="mini primary"
              @click="confirmAndUpdate(o.id, 'COMPLETED', '完成')"
            >
              完成
            </button>
            <button
              v-if="role === 'RESTAURANT' && (o.status === 'PAID' || o.status === 'DELIVERING')"
              class="mini danger"
              @click="confirmAndUpdate(o.id, 'CANCELED', '取消')"
            >
              取消
            </button>
          </td>
        </tr>
        <tr v-if="!loading && orders.length === 0">
          <td colspan="9" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="9" class="empty">加载中...</td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <button :disabled="page <= 1" @click="loadOrders(page - 1)">上一页</button>
      <span>第 {{ page }} 页，共 {{ total }} 条</span>
      <button :disabled="page * size >= total" @click="loadOrders(page + 1)">下一页</button>
    </div>

    <div v-if="detail" class="detail">
      <div class="detail-head">
        <h3>订单详情 #{{ detail.id }}</h3>
        <button class="close" @click="detail = null">关闭</button>
      </div>

      <div class="detail-grid">
        <div><b>饭店</b>：{{ detail.restaurantName }}</div>
        <div><b>食客</b>：{{ detail.customerUsername }}</div>
        <div><b>状态</b>：{{ detail.status }}</div>
        <div>
          <b>支付状态</b>：{{ detail.payStatus }}
          <span v-if="detail.payStatus === 'REFUNDED'" class="tag warn">退款</span>
        </div>
        <div><b>总金额</b>：{{ detail.totalAmount ?? 0 }}</div>
        <div><b>实付金额</b>：{{ detail.payAmount ?? 0 }}</div>
        <div><b>平台抽成</b>：{{ detail.commissionAmount ?? 0 }}</div>
        <div v-if="detail.refundedAt"><b>退款时间</b>：{{ formatTime(detail.refundedAt) }}</div>
        <div v-if="detail.cancelReason"><b>取消原因</b>：{{ detail.cancelReason }}</div>
        <div v-if="detail.deliveryLat != null && detail.deliveryLng != null">
          <b>订单坐标</b>：{{ detail.deliveryLat.toFixed(6) }}, {{ detail.deliveryLng.toFixed(6) }}
        </div>
        <div v-if="detail.addressDetail"><b>地址</b>：{{ detail.addressDetail }}</div>
        <div v-if="detail.contactName || detail.contactPhone">
          <b>联系人</b>：{{ detail.contactName }} {{ detail.contactPhone }}
        </div>
        <div v-if="detail.deliveryStaffName || detail.deliveryStaffPhone">
          <b>骑手</b>：{{ detail.deliveryStaffName }} {{ detail.deliveryStaffPhone }}
        </div>
        <div v-else>
          <b>骑手</b>：未分配
        </div>
        <div v-if="detail.deliveryStaffLat != null && detail.deliveryStaffLng != null">
          <b>骑手坐标</b>：{{ detail.deliveryStaffLat.toFixed(6) }}, {{ detail.deliveryStaffLng.toFixed(6) }}
        </div>
        <div v-if="detail.deliveryStaffLoad != null"><b>骑手负载</b>：{{ detail.deliveryStaffLoad }}</div>
        <div v-if="detail.deliveryStaffLocationUpdatedAt"><b>位置上报</b>：{{ detail.deliveryStaffLocationUpdatedAt }}</div>
        <div v-if="distanceKm != null"><b>直线距离</b>：{{ distanceKm.toFixed(2) }} km（估算）</div>
      </div>

      <div v-if="role === 'ADMIN' && (detail.status === 'PAID' || detail.status === 'DELIVERING')" class="assign-row">
        <label>
          分配骑手
          <select v-model.number="assignStaffId">
            <option :value="0">自动分配（按当前负载最小）</option>
            <option v-for="s in assignableStaff" :key="s.id" :value="s.id">
              #{{ s.id }} {{ s.name }}（负载 {{ s.currentLoad ?? 0 }}）
            </option>
          </select>
        </label>
        <button :disabled="assigning" @click="assignDelivery">分配</button>
      </div>

      <div class="status-row">
        <label>
          修改状态
          <select v-model="nextStatus">
            <option value="CREATED">CREATED</option>
            <option value="PAID">PAID</option>
            <option value="DELIVERING">DELIVERING</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="CANCELED">CANCELED</option>
          </select>
        </label>
        <button @click="updateStatus">保存</button>
      </div>

      <h4 class="sub">订单明细</h4>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

interface OrderListItem {
  id: number
  restaurantId: number
  restaurantName: string
  customerId: number
  customerUsername: string
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
  customerId: number
  customerUsername: string
  status: string
  payStatus: string
  totalAmount: number
  payAmount: number
  commissionAmount: number
  createdAt: string
  paidAt: string | null
  finishedAt: string | null
  refundedAt: string | null
  cancelReason: string | null
  addressDetail: string | null
  deliveryLat: number | null
  deliveryLng: number | null
  contactName: string | null
  contactPhone: string | null
  deliveryStaffId: number | null
  deliveryStaffName: string | null
  deliveryStaffPhone: string | null
  deliveryStaffLat: number | null
  deliveryStaffLng: number | null
  deliveryStaffLoad: number | null
  deliveryStaffLocationUpdatedAt: string | null
  items: OrderItemRow[]
}

interface DeliveryStaff {
  id: number
  name: string
  phone: string | null
  status: string | null
  currentLoad: number | null
}

const loading = ref(false)
const orders = ref<OrderListItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const filters = ref({
  status: '',
  payStatus: '',
  start: '',
  end: '',
})

const detail = ref<OrderDetail | null>(null)
const distanceKm = computed(() => {
  const d = detail.value
  if (!d) return null
  if (d.deliveryLat == null || d.deliveryLng == null) return null
  if (d.deliveryStaffLat == null || d.deliveryStaffLng == null) return null
  return haversineKm(d.deliveryStaffLat, d.deliveryStaffLng, d.deliveryLat, d.deliveryLng)
})

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number) {
  const r = 6371.0088
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2)
  const c = 2 * Math.asin(Math.min(1, Math.sqrt(a)))
  return r * c
}
const nextStatus = ref('CREATED')
const deliveryStaff = ref<DeliveryStaff[]>([])
const assignStaffId = ref<number>(0)
const assigning = ref(false)

const role = computed(() => localStorage.getItem('role') || '')
const restaurantId = computed<number | null>(() => {
  try {
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const v = user?.restaurantId
    return typeof v === 'number' ? v : v ? Number(v) : null
  } catch {
    return null
  }
})

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

const assignableStaff = computed(() =>
  deliveryStaff.value.filter((s) => (s.status ?? '').toUpperCase() === 'ACTIVE'),
)

async function loadDeliveryStaff() {
  const res = await fetch('http://localhost:8081/api/delivery-staff')
  if (!res.ok) return
  deliveryStaff.value = await res.json()
}

async function loadOrders(targetPage: number) {
  loading.value = true
  try {
    const qs = new URLSearchParams()
    qs.set('page', String(targetPage))
    qs.set('size', String(size.value))
    if (filters.value.status) qs.set('status', filters.value.status)
    if (filters.value.payStatus) qs.set('payStatus', filters.value.payStatus)
    if (filters.value.start) qs.set('start', filters.value.start)
    if (filters.value.end) qs.set('end', filters.value.end)
    if (role.value === 'RESTAURANT' && restaurantId.value != null) {
      qs.set('restaurantId', String(restaurantId.value))
    }

    const res = await fetch(`http://localhost:8081/api/orders?${qs.toString()}`)
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
  const res = await fetch(`http://localhost:8081/api/orders/${id}`)
  if (!res.ok) return
  detail.value = (await res.json()) as OrderDetail
  nextStatus.value = detail.value.status
  assignStaffId.value = 0
}

async function assignDelivery() {
  if (!detail.value) return
  assigning.value = true
  try {
    const body =
      assignStaffId.value && assignStaffId.value > 0 ? { deliveryStaffId: assignStaffId.value } : null
    const res = await fetch(`http://localhost:8081/api/orders/${detail.value.id}/assign-delivery`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : null,
    })
    if (!res.ok) return
    await openDetail(detail.value.id)
    await loadOrders(page.value)
  } finally {
    assigning.value = false
  }
}

async function quickUpdateStatus(id: number, status: string) {
  const res = await fetch(`http://localhost:8081/api/orders/${id}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
  if (res.ok) {
    await loadOrders(page.value)
    if (detail.value?.id === id) {
      detail.value = (await res.json()) as OrderDetail
      nextStatus.value = detail.value.status
    }
  }
}

function confirmAndUpdate(id: number, status: string, actionLabel: string) {
  if (!confirm(`确认要${actionLabel}该订单吗？`)) return
  void quickUpdateStatus(id, status)
}

async function updateStatus() {
  if (!detail.value) return
  const res = await fetch(`http://localhost:8081/api/orders/${detail.value.id}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: nextStatus.value }),
  })
  if (res.ok) {
    detail.value = (await res.json()) as OrderDetail
    await loadOrders(page.value)
  }
}

onMounted(() => {
  if (role.value === 'RESTAURANT' && !filters.value.status) {
    filters.value.status = 'PAID'
  }
  void loadDeliveryStaff()
  void loadOrders(1)
})
</script>

<style scoped>
.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: end;
  margin-bottom: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #333;
}

input,
select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

button {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #1890ff;
  background-color: #1890ff;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.table th,
.table td {
  border: 1px solid #f0f0f0;
  padding: 8px 10px;
  text-align: left;
}

.table thead {
  background-color: #fafafa;
}

.empty {
  text-align: center;
  color: #999;
}

.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mini {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #333;
}

.mini.primary {
  border-color: #1890ff;
  background-color: #1890ff;
  color: #fff;
}

.mini.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
  background: #fff;
}

.pager {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.pager button {
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
}

.detail {
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.close {
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 8px 12px;
  margin: 12px 0;
  font-size: 13px;
}

.status-row {
  display: flex;
  gap: 10px;
  align-items: end;
  margin-bottom: 12px;
}

.assign-row {
  display: flex;
  gap: 10px;
  align-items: end;
  margin-bottom: 12px;
}

.sub {
  margin: 8px 0;
}

.tag {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  font-size: 12px;
  color: #666;
  background: #fff;
}

.tag.warn {
  border-color: #ffe58f;
  color: #d48806;
}
</style>
