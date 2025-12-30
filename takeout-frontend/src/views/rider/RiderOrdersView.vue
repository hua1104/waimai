<template>
  <div class="page">
    <header class="topbar">
      <div class="title">骑手端</div>
      <div class="actions">
        <span class="user">骑手</span>
        <button class="btn" @click="router.push('/d/hall')">接单大厅</button>
        <button class="btn" @click="reportLocation">上报当前位置</button>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card">
        <div v-if="!deliveryStaffId" class="warn">当前账号缺少 deliveryStaffId，请重新用“骑手”角色登录。</div>

        <div v-else>
          <div class="statsbar">
            <div class="stat">
              <div class="k">当前负载</div>
              <div class="v">{{ stats?.currentLoad ?? '-' }}</div>
            </div>
            <div class="stat">
              <div class="k">配送中</div>
              <div class="v">{{ stats?.deliveringCount ?? '-' }}</div>
            </div>
            <div class="stat">
              <div class="k">今日完成</div>
              <div class="v">{{ stats?.completedTodayCount ?? '-' }}</div>
            </div>
            <div class="stat">
              <div class="k">今日收入(模拟)</div>
              <div class="v">¥ {{ stats?.incomeToday != null ? stats.incomeToday.toFixed(2) : '-' }}</div>
            </div>
            <div class="stat actions">
              <button class="btn" @click="refreshAll" :disabled="statsLoading">
                {{ statsLoading ? '刷新中...' : '刷新' }}
              </button>
            </div>
          </div>
          <div v-if="locationMsg" class="msg">{{ locationMsg }}</div>
          <div class="filters">
            <label>
              状态
              <select v-model="filters.status">
                <option value="">全部</option>
                <option value="PAID">PAID</option>
                <option value="DELIVERING">DELIVERING</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="CANCELED">CANCELED</option>
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
            <button class="btn primary" @click="loadOrders(1)">查询</button>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>饭店</th>
                <th>食客</th>
                <th>地址</th>
                <th>联系</th>
                <th>状态</th>
                <th>支付</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id">
                <td>{{ o.id }}</td>
                <td>{{ o.restaurantName }}</td>
                <td>{{ o.customerUsername }}</td>
                <td class="addr">{{ o.addressDetail }}</td>
                <td>{{ o.contactName }} {{ o.contactPhone }}</td>
                <td>{{ o.status }}</td>
                <td>{{ o.payStatus }}</td>
                <td class="actions-cell">
                  <button class="btn" @click="openDetail(o.id)">详情</button>
                  <button
                    v-if="o.status === 'PAID'"
                    class="btn primary"
                    @click="confirmStartDelivering(o.id)"
                  >
                    开始配送
                  </button>
                  <button
                    v-if="o.status === 'DELIVERING'"
                    class="btn primary"
                    @click="confirmComplete(o.id)"
                  >
                    送达完成
                  </button>
                  <button v-if="o.status === 'DELIVERING'" class="btn danger" @click="confirmCancel(o.id)">
                    取消配送
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
              <div><b>食客</b>：{{ detail.customerUsername }}</div>
              <div><b>状态</b>：{{ detail.status }}</div>
              <div><b>地址</b>：{{ detail.addressDetail }}</div>
              <div v-if="detail.deliveryLat != null && detail.deliveryLng != null">
                <b>订单坐标</b>：{{ detail.deliveryLat.toFixed(6) }}, {{ detail.deliveryLng.toFixed(6) }}
              </div>
              <div><b>联系人</b>：{{ detail.contactName }} {{ detail.contactPhone }}</div>
              <div v-if="detail.remark"><b>备注</b>：{{ detail.remark }}</div>
            </div>

            <div class="route">
              <h4 class="sub">配送路线（路网优先）</h4>
              <div v-if="routeError" class="warn">{{ routeError }}</div>
              <div v-else-if="routeLoading" class="muted">加载路线中...</div>
              <div v-else-if="route">
                <div class="route-row">
                  <b>骑手 → 饭店</b>：
                  {{ route.legs.riderToRestaurant.distanceKm.toFixed(2) }} km
                  <span v-if="route.legs.riderToRestaurant.durationMin != null">
                    ，约 {{ route.legs.riderToRestaurant.durationMin.toFixed(0) }} 分钟
                  </span>
                  <span class="tag">{{ route.legs.riderToRestaurant.source }}</span>
                </div>
                <div class="route-row">
                  <b>饭店 → 收货</b>：
                  {{ route.legs.restaurantToCustomer.distanceKm.toFixed(2) }} km
                  <span v-if="route.legs.restaurantToCustomer.durationMin != null">
                    ，约 {{ route.legs.restaurantToCustomer.durationMin.toFixed(0) }} 分钟
                  </span>
                  <span class="tag">{{ route.legs.restaurantToCustomer.source }}</span>
                </div>
                <div class="route-row">
                  <b>骑手 → 收货(直达)</b>：
                  {{ route.legs.riderToCustomer.distanceKm.toFixed(2) }} km
                  <span v-if="route.legs.riderToCustomer.durationMin != null">
                    ，约 {{ route.legs.riderToCustomer.durationMin.toFixed(0) }} 分钟
                  </span>
                  <span class="tag">{{ route.legs.riderToCustomer.source }}</span>
                </div>
                <div v-if="!route.akEnabled" class="muted">当前后端未配置百度 AK，已使用直线距离估算。</div>
              </div>
              <div v-else class="muted">暂无路线信息</div>
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
          </div>

          <div v-if="msg" class="msg">{{ msg }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

interface OrderListItem {
  id: number
  restaurantId: number
  restaurantName: string
  customerId: number
  customerUsername: string
  deliveryStaffId: number
  status: string
  payStatus: string
  payAmount: number
  createdAt: string
  paidAt: string
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
  deliveryStaffId: number
  status: string
  payStatus: string
  totalAmount: number
  payAmount: number
  createdAt: string
  paidAt: string
  finishedAt: string
  remark: string | null
  addressDetail: string | null
  deliveryLat: number | null
  deliveryLng: number | null
  contactName: string | null
  contactPhone: string | null
  items: OrderItemRow[]
}

interface RiderStats {
  deliveryStaffId: number
  currentLoad: number
  deliveringCount: number
  completedTodayCount: number
  feePerOrder: number
  incomeToday: number
  currentLat: number | null
  currentLng: number | null
  locationUpdatedAt: string | null
}

interface RouteLeg {
  distanceKm: number
  durationMin: number | null
  source: string
}

interface RouteResponse {
  akEnabled: boolean
  legs: {
    riderToRestaurant: RouteLeg
    restaurantToCustomer: RouteLeg
    riderToCustomer: RouteLeg
  }
}

const router = useRouter()
const loading = ref(false)
const orders = ref<OrderListItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const msg = ref('')
const locationMsg = ref('')
const detail = ref<OrderDetail | null>(null)
const stats = ref<RiderStats | null>(null)
const statsLoading = ref(false)
const route = ref<RouteResponse | null>(null)
const routeLoading = ref(false)
const routeError = ref('')
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
  status: 'DELIVERING',
  start: '',
  end: '',
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

async function loadStats() {
  if (!deliveryStaffId.value) return
  statsLoading.value = true
  try {
    const res = await fetch(api(`/api/rider/stats?deliveryStaffId=${encodeURIComponent(String(deliveryStaffId.value))}`))
    if (!res.ok) return
    stats.value = (await res.json()) as RiderStats
  } finally {
    statsLoading.value = false
  }
}

async function loadRoute(orderId: number) {
  if (!deliveryStaffId.value) return
  routeError.value = ''
  routeLoading.value = true
  try {
    const res = await fetch(
      api(`/api/rider/orders/${orderId}/route?deliveryStaffId=${encodeURIComponent(String(deliveryStaffId.value))}`),
    )
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      route.value = null
      routeError.value = data.message ?? '路线加载失败'
      return
    }
    route.value = data as RouteResponse
  } finally {
    routeLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadStats(), loadOrders(page.value)])
}

const BAIDU_AK = (import.meta as any).env?.VITE_BAIDU_MAP_AK as string | undefined

function ensureBMap(): Promise<void> {
  const w = window as any
  if (!BAIDU_AK) return Promise.reject(new Error('missing ak'))
  if (w.BMap) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const cbName = '__baiduMapLoadedRider__'
    w[cbName] = () => resolve()
    const script = document.createElement('script')
    script.src = `https://api.map.baidu.com/api?v=3.0&ak=${encodeURIComponent(String(BAIDU_AK))}&callback=${cbName}`
    script.async = true
    script.onerror = () => reject(new Error('load baidu map failed'))
    document.head.appendChild(script)
  })
}

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

async function reportLocation() {
  locationMsg.value = ''
  if (!deliveryStaffId.value) {
    locationMsg.value = '当前账号缺少 deliveryStaffId'
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
          body: JSON.stringify({
            deliveryStaffId: deliveryStaffId.value,
            lat,
            lng,
          }),
        })
        const data = await res.json().catch(() => ({}))
        if (!res.ok) {
          locationMsg.value = data.message ?? '上报失败'
          return
        }
        locationMsg.value = '已上报当前位置'
        await loadStats()
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
  if (!deliveryStaffId.value) return
  loading.value = true
  msg.value = ''
  try {
    const qs = new URLSearchParams()
    qs.set('deliveryStaffId', String(deliveryStaffId.value))
    qs.set('page', String(targetPage))
    qs.set('size', String(size.value))
    if (filters.value.status) qs.set('status', filters.value.status)
    if (filters.value.start) qs.set('start', filters.value.start)
    if (filters.value.end) qs.set('end', filters.value.end)

    const res = await fetch(api(`/api/rider/orders?${qs.toString()}`))
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
  if (!deliveryStaffId.value) return
  const res = await fetch(api(`/api/rider/orders/${id}?deliveryStaffId=${deliveryStaffId.value}`))
  if (!res.ok) return
  detail.value = (await res.json()) as OrderDetail
  route.value = null
  await loadRoute(id)
}

async function confirmComplete(id: number) {
  await updateOrderStatus(id, 'COMPLETED', '确认已送达并完成该订单？', '已完成订单')
}

async function confirmCancel(id: number) {
  if (!deliveryStaffId.value) return
  if (!confirm('确认取消配送并释放该订单（其他骑手可重新抢单）？')) return
  msg.value = ''
  const res = await fetch(api(`/api/rider/orders/${id}/cancel?deliveryStaffId=${deliveryStaffId.value}`), {
    method: 'POST',
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '操作失败'
    return
  }
  msg.value = '已取消配送，订单已释放'
  if (detail.value?.id === id) detail.value = null
  await loadOrders(page.value)
  await loadStats()
}

async function confirmStartDelivering(id: number) {
  await updateOrderStatus(id, 'DELIVERING', '确认开始配送该订单？', '已开始配送')
}

async function updateOrderStatus(
  id: number,
  status: 'DELIVERING' | 'COMPLETED',
  confirmText: string,
  successText: string,
) {
  if (!deliveryStaffId.value) return
  if (!confirm(confirmText)) return
  msg.value = ''
  const res = await fetch(api(`/api/rider/orders/${id}/status?deliveryStaffId=${deliveryStaffId.value}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '操作失败'
    return
  }
  msg.value = successText
  if (detail.value?.id === id) detail.value = null
  await loadOrders(page.value)
  await loadStats()
}

onMounted(async () => {
  await refreshAll()
  timer = window.setInterval(() => {
    loadStats()
  }, 10_000)
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

.statsbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
  align-items: stretch;
}

.stat {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 10px 12px;
  min-width: 140px;
  background: #fafafa;
}

.stat .k {
  font-size: 12px;
  color: #666;
}

.stat .v {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 700;
}

.stat.actions {
  display: flex;
  align-items: center;
  background: transparent;
  border: none;
  padding: 0;
}

.route {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #f0f0f0;
}

.route-row {
  margin-top: 6px;
  font-size: 13px;
}

.tag {
  margin-left: 8px;
  font-size: 12px;
  padding: 2px 6px;
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  color: #555;
  background: #fff;
}

.muted {
  margin-top: 6px;
  color: #999;
  font-size: 13px;
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
  max-width: 260px;
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

.detail {
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
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

.sub {
  margin: 10px 0 6px;
}

.msg {
  margin-top: 10px;
  color: #389e0d;
  font-size: 13px;
}
</style>
