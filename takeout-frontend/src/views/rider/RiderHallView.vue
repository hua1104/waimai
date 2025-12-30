<template>
  <div class="page">
    <header class="topbar">
      <div class="title">接单大厅</div>
      <div class="actions">
        <button class="btn" @click="reportLocation">上报当前位置</button>
        <label class="toggle">
          <input type="checkbox" v-model="autoRefresh" />
          自动刷新
        </label>
        <button class="btn" @click="goMyOrders">我的订单</button>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card">
        <div v-if="!deliveryStaffId" class="warn">缺少骑手信息（deliveryStaffId），请重新登录。</div>

        <div v-else>
          <div v-if="locationMsg" class="msg">{{ locationMsg }}</div>
          <div class="filters">
            <label>
              起始日期
              <input v-model="filters.start" type="date" />
            </label>
            <label>
              截止日期
              <input v-model="filters.end" type="date" />
            </label>
            <button class="btn primary" @click="loadOrders(1)">查询</button>
            <div class="muted" v-if="lastUpdatedAt">上次刷新：{{ lastUpdatedAt }}</div>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>订单ID</th>
                <th>饭店</th>
                <th>食客</th>
                <th>金额</th>
                <th>地址</th>
                <th>联系人</th>
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
                <td class="addr">{{ o.addressDetail }}</td>
                <td>{{ o.contactName }} {{ o.contactPhone }}</td>
                <td>{{ formatTime(o.paidAt) }}</td>
                <td class="actions-cell">
                  <button class="btn primary" :disabled="takingId === o.id" @click="takeOrder(o.id)">
                    {{ takingId === o.id ? '抢单中...' : '抢单' }}
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && orders.length === 0">
                <td colspan="8" class="empty">暂无可抢订单</td>
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

          <div v-if="msg" class="msg">{{ msg }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

interface HallOrderListItem {
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
  addressDetail: string | null
  contactName: string | null
  contactPhone: string | null
}

interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

const router = useRouter()
const loading = ref(false)
const orders = ref<HallOrderListItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const msg = ref('')
const takingId = ref<number | null>(null)
const locationMsg = ref('')
const lastUpdatedAt = ref<string>('')
const autoRefresh = ref(true)
let timer: number | null = null

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('currentUser') || '{}')
  } catch {
    return {}
  }
})

const deliveryStaffId = computed<number | null>(() => {
  const v = currentUser.value?.deliveryStaffId
  return typeof v === 'number' ? v : v ? Number(v) : null
})

const filters = ref({
  start: '',
  end: '',
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

const BAIDU_AK = (import.meta as any).env?.VITE_BAIDU_MAP_AK as string | undefined

function ensureBMap(): Promise<void> {
  const w = window as any
  if (!BAIDU_AK) return Promise.reject(new Error('missing ak'))
  if (w.BMap) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const cbName = '__baiduMapLoadedRiderHall__'
    w[cbName] = () => resolve()
    const script = document.createElement('script')
    script.src = `https://api.map.baidu.com/api?v=3.0&ak=${encodeURIComponent(String(BAIDU_AK))}&callback=${cbName}`
    script.async = true
    script.onerror = () => reject(new Error('load baidu map failed'))
    document.head.appendChild(script)
  })
}

function formatTime(v?: string | null) {
  if (!v) return ''
  try {
    return new Date(v).toLocaleString()
  } catch {
    return v
  }
}

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

function goMyOrders() {
  router.push('/d')
}

async function reportLocation() {
  locationMsg.value = ''
  if (!deliveryStaffId.value) {
    locationMsg.value = '缺少骑手信息 deliveryStaffId'
    return
  }
  if (!navigator.geolocation) {
    locationMsg.value = '浏览器不支持定位'
    return
  }

  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      try {
        let lat = pos.coords.latitude
        let lng = pos.coords.longitude

        try {
          await ensureBMap()
          const w = window as any
          const convertor = new w.BMap.Convertor()
          const rawPt = new w.BMap.Point(lng, lat)
          const converted = await new Promise<{ lat: number; lng: number }>((resolve) => {
            convertor.translate([rawPt], 1, 5, (data: any) => {
              const pt = data?.points?.[0] ?? rawPt
              resolve({ lat: Number(pt.lat), lng: Number(pt.lng) })
            })
          })
          lat = converted.lat
          lng = converted.lng
        } catch {}

        const res = await fetch(api('/api/rider/location'), {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deliveryStaffId: deliveryStaffId.value, lat, lng }),
        })
        const data = await res.json().catch(() => ({}))
        if (!res.ok) {
          locationMsg.value = data.message ?? '上报失败'
          return
        }
        locationMsg.value = '已上报当前位置'
      } catch {
        locationMsg.value = '无法连接到服务器'
      }
    },
    () => {
      locationMsg.value = '定位失败，请检查浏览器权限'
    },
    { enableHighAccuracy: true, timeout: 8000 },
  )
}

async function loadOrders(targetPage: number) {
  loading.value = true
  msg.value = ''
  try {
    const qs = new URLSearchParams()
    qs.set('page', String(targetPage))
    qs.set('size', String(size.value))
    if (filters.value.start) qs.set('start', filters.value.start)
    if (filters.value.end) qs.set('end', filters.value.end)

    const res = await fetch(api(`/api/rider/hall-orders?${qs.toString()}`))
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      msg.value = data.message ?? '加载失败'
      return
    }
    const data = (await res.json()) as PageResponse<HallOrderListItem>
    orders.value = data.items
    page.value = data.page
    size.value = data.size
    total.value = data.total
    lastUpdatedAt.value = new Date().toLocaleTimeString()
  } finally {
    loading.value = false
  }
}

async function takeOrder(orderId: number) {
  if (!deliveryStaffId.value) return
  if (!confirm(`确认抢单 #${orderId} 并立即开始配送？`)) return
  msg.value = ''
  takingId.value = orderId
  try {
    const res = await fetch(
      api(`/api/rider/orders/${orderId}/take?deliveryStaffId=${encodeURIComponent(String(deliveryStaffId.value))}`),
      { method: 'POST' },
    )
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      msg.value = data.message ?? '抢单失败'
      return
    }
    msg.value = '抢单成功，已进入配送中'
    await loadOrders(page.value)
    router.push('/d')
  } finally {
    takingId.value = null
  }
}

function startTimer() {
  if (timer != null) window.clearInterval(timer)
  if (!autoRefresh.value) return
  timer = window.setInterval(() => {
    loadOrders(page.value)
  }, 10_000)
}

watch(autoRefresh, () => startTimer())

onMounted(() => {
  loadOrders(1)
  startTimer()
})

onUnmounted(() => {
  if (timer != null) window.clearInterval(timer)
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.title {
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #333;
}

.muted {
  color: #999;
  font-size: 12px;
  align-self: center;
}

.content {
  padding: 12px;
}

.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 6px;
}

.warn {
  padding: 10px 12px;
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #614700;
  border-radius: 6px;
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

.btn {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  text-decoration: none;
  color: #333;
}

.btn.primary {
  border-color: #1890ff;
  background: #1890ff;
  color: #fff;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
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

.addr {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.actions-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty {
  text-align: center;
  color: #999;
}

.pager {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.msg {
  margin-top: 10px;
  color: #389e0d;
  font-size: 13px;
}
</style>
