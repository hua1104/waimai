<template>
  <div class="page">
    <header class="topbar">
      <div class="title">我的统计（食客端）</div>
      <div class="actions">
        <RouterLink class="btn" to="/c">返回点餐</RouterLink>
        <RouterLink class="btn" to="/c/profile">个人信息</RouterLink>
        <RouterLink class="btn" to="/c/orders">我的订单</RouterLink>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card">
        <div class="filters">
          <label>
            开始日期
            <input v-model="start" type="date" />
          </label>
          <label>
            结束日期
            <input v-model="end" type="date" />
          </label>
          <button class="btn primary" @click="load">查询</button>
        </div>

        <div class="summary">
          <div class="box">
            <div class="k">订单数</div>
            <div class="v">{{ data?.totalOrders ?? 0 }}</div>
          </div>
          <div class="box">
            <div class="k">消费金额（已支付）</div>
            <div class="v">¥{{ Number(data?.totalSpend ?? 0).toFixed(2) }}</div>
          </div>
        </div>

        <h3 class="sub">订单状态分布</h3>
        <div class="chips">
          <div v-for="s in data?.statusCounts ?? []" :key="s.status" class="chip">
            <span class="name">{{ s.status }}</span>
            <span class="count">{{ s.count }}</span>
          </div>
          <div v-if="!loading && (data?.statusCounts?.length ?? 0) === 0" class="empty">暂无数据</div>
        </div>

        <div class="grid2">
          <div>
            <h3 class="sub">常点饭店 TOP</h3>
            <table class="table">
              <thead>
                <tr>
                  <th>饭店</th>
                  <th>消费额</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in data?.topRestaurants ?? []" :key="r.restaurantId">
                  <td>{{ r.restaurantName }}</td>
                  <td>¥{{ Number(r.salesAmount ?? 0).toFixed(2) }}</td>
                </tr>
                <tr v-if="!loading && (data?.topRestaurants?.length ?? 0) === 0">
                  <td colspan="2" class="empty">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div>
            <h3 class="sub">常点菜品 TOP</h3>
            <table class="table">
              <thead>
                <tr>
                  <th>菜品</th>
                  <th>数量</th>
                  <th>消费额</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in data?.topDishes ?? []" :key="d.dishId">
                  <td>{{ d.dishName }}</td>
                  <td>{{ d.quantity }}</td>
                  <td>¥{{ Number(d.salesAmount ?? 0).toFixed(2) }}</td>
                </tr>
                <tr v-if="!loading && (data?.topDishes?.length ?? 0) === 0">
                  <td colspan="3" class="empty">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="loading" class="empty">加载中...</div>
        <div v-if="msg" class="msg">{{ msg }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

interface StatusCountRow {
  status: string
  count: number
}

interface TopRestaurantRow {
  restaurantId: number
  restaurantName: string
  salesAmount: number
}

interface TopDishRow {
  dishId: number
  dishName: string
  quantity: number
  salesAmount: number
}

interface SummaryResponse {
  totalSpend: number
  totalOrders: number
  statusCounts: StatusCountRow[]
  topRestaurants: TopRestaurantRow[]
  topDishes: TopDishRow[]
}

const router = useRouter()
const loading = ref(false)
const msg = ref('')
const data = ref<SummaryResponse | null>(null)

const today = new Date()
const end = ref(today.toISOString().slice(0, 10))
const startDate = new Date(today.getTime() - 30 * 24 * 3600 * 1000)
const start = ref(startDate.toISOString().slice(0, 10))

const customerId = computed<number | null>(() => {
  try {
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const v = user?.userId
    return typeof v === 'number' ? v : v ? Number(v) : null
  } catch {
    return null
  }
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

async function load() {
  if (!customerId.value) return
  loading.value = true
  msg.value = ''
  try {
    const qs = new URLSearchParams()
    qs.set('customerId', String(customerId.value))
    qs.set('start', start.value)
    qs.set('end', end.value)
    const res = await fetch(api(`/api/customer/stats/summary?${qs.toString()}`))
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      msg.value = err.message ?? '查询失败'
      return
    }
    data.value = await res.json()
  } finally {
    loading.value = false
  }
}

onMounted(load)
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

input {
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

.summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.box {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
}

.k {
  font-size: 12px;
  color: #666;
}

.v {
  font-size: 20px;
  font-weight: 700;
  margin-top: 6px;
}

.sub {
  margin: 10px 0 8px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.chip {
  border: 1px solid #f0f0f0;
  border-radius: 999px;
  padding: 6px 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}

.chip .name {
  color: #333;
}

.chip .count {
  color: #1890ff;
  font-weight: 700;
}

.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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

.empty {
  text-align: center;
  color: #999;
  padding: 10px 0;
}

.msg {
  margin-top: 10px;
  color: #ff4d4f;
  font-size: 13px;
}
</style>
