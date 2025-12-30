<template>
  <div class="wrap">
    <div class="head">
      <div>
        <h2>仪表盘</h2>
        <div class="sub">统计区间：{{ rangeLabel }}</div>
      </div>
      <div class="filters">
        <label>
          开始日期
          <input v-model="filters.start" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="filters.end" type="date" />
        </label>
        <button @click="load">刷新</button>
      </div>
    </div>

    <div v-if="role !== 'ADMIN'" class="card">
      <h3>提示</h3>
      <div class="sub">当前仪表盘主要面向管理员，其他角色请使用左侧对应菜单。</div>
    </div>

    <div v-else>
      <div class="cards">
        <div class="card kpi">
          <div class="label">饭店数</div>
          <div class="value">{{ data?.restaurantCount ?? 0 }}</div>
        </div>
        <div class="card kpi">
          <div class="label">食客数</div>
          <div class="value">{{ data?.customerCount ?? 0 }}</div>
        </div>
        <div class="card kpi">
          <div class="label">骑手数</div>
          <div class="value">{{ data?.deliveryStaffCount ?? 0 }}</div>
        </div>
        <div class="card kpi">
          <div class="label">订单数</div>
          <div class="value">{{ data?.orderCount ?? 0 }}</div>
        </div>
        <div class="card kpi">
          <div class="label">销售额</div>
          <div class="value">{{ money(data?.totalSales) }}</div>
        </div>
        <div class="card kpi">
          <div class="label">平台收入(抽成)</div>
          <div class="value">{{ money(data?.platformIncome) }}</div>
        </div>
      </div>

      <div class="grid">
        <div class="card">
          <div class="card-head">
            <h3>订单状态分布</h3>
            <button class="mini" @click="router.push('/orders')">去订单管理</button>
          </div>
          <div v-if="loading" class="empty">加载中...</div>
          <div v-else-if="(data?.statusCounts?.length ?? 0) === 0" class="empty">暂无数据</div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>状态</th>
                <th>数量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in data?.statusCounts ?? []" :key="s.status">
                <td>{{ s.status }}</td>
                <td>{{ s.count }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>最近订单</h3>
            <button class="mini" @click="router.push('/orders')">查看全部</button>
          </div>
          <div v-if="loading" class="empty">加载中...</div>
          <div v-else-if="(data?.recentOrders?.length ?? 0) === 0" class="empty">暂无数据</div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>饭店</th>
                <th>食客</th>
                <th>金额</th>
                <th>状态</th>
                <th>支付</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in data?.recentOrders ?? []" :key="o.id">
                <td>{{ o.id }}</td>
                <td>{{ o.restaurantName }}</td>
                <td>{{ o.customerUsername }}</td>
                <td>{{ o.payAmount ?? 0 }}</td>
                <td>{{ o.status }}</td>
                <td>{{ o.payStatus }}</td>
                <td>{{ formatTime(o.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="message" class="msg">{{ message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const role = ref(localStorage.getItem('role') ?? '')

interface StatusCountRow {
  status: string
  count: number
}

interface RecentOrderRow {
  id: number
  restaurantName: string
  customerUsername: string
  status: string
  payStatus: string
  payAmount: number | null
  createdAt: string
}

interface DashboardResponse {
  start: string
  end: string
  restaurantCount: number
  customerCount: number
  deliveryStaffCount: number
  orderCount: number
  totalSales: number
  platformIncome: number
  statusCounts: StatusCountRow[]
  recentOrders: RecentOrderRow[]
}

const loading = ref(false)
const message = ref('')
const data = ref<DashboardResponse | null>(null)

const filters = ref({
  start: '',
  end: '',
})

const rangeLabel = computed(() => {
  const start = filters.value.start || data.value?.start
  const end = filters.value.end || data.value?.end
  if (!start && !end) return '最近7天'
  if (start && end) return `${start} ~ ${end}`
  return start ? `从 ${start}` : `到 ${end}`
})

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

function money(v: number | null | undefined) {
  const n = Number(v ?? 0)
  if (Number.isNaN(n)) return '0'
  return n.toFixed(2)
}

function dateToInput(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function load() {
  loading.value = true
  message.value = ''
  try {
    const qs = new URLSearchParams()
    if (filters.value.start) qs.set('start', filters.value.start)
    if (filters.value.end) qs.set('end', filters.value.end)
    const res = await fetch(`http://localhost:8081/api/admin/dashboard?${qs.toString()}`)
    if (!res.ok) {
      message.value = '加载失败'
      return
    }
    data.value = (await res.json()) as DashboardResponse
  } catch {
    message.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  filters.value.start = dateToInput(start)
  filters.value.end = dateToInput(end)
  void load()
})
</script>

<style scoped>
.wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.sub {
  color: #666;
  font-size: 13px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: end;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #333;
}

input {
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

.cards {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.kpi .label {
  color: #666;
  font-size: 13px;
}

.kpi .value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 6px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.mini {
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  font-size: 12px;
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
  color: #888;
  padding: 18px 0;
}

.msg {
  color: #ff4d4f;
}

@media (max-width: 1200px) {
  .cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
