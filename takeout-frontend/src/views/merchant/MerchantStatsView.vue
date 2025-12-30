<template>
  <div class="card">
    <h2>仪表盘&本店统计（饭店端）</h2>

    <div v-if="!restaurantId" class="warn">当前账号缺少 restaurantId，请重新用“饭店”角色登录。</div>

    <div v-else>
      <div class="quick">
        <button class="link" @click="go('/orders')">订单管理</button>
        <button class="link" @click="go('/merchant/dishes')">菜品管理</button>
        <button class="link" @click="go('/merchant/promotions')">优惠活动</button>
      </div>

      <div class="filters">
        <label>
          开始日期
          <input v-model="start" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="end" type="date" />
        </label>
        <button @click="load">查询</button>
      </div>

      <div class="summary">
        <div class="box">
          <div class="k">销售额</div>
          <div class="v">¥{{ Number(data?.salesAmount ?? 0).toFixed(2) }}</div>
        </div>
      </div>

      <h3 class="sub">热销菜品 TOP</h3>
      <table class="table">
        <thead>
          <tr>
            <th>菜品</th>
            <th>销量</th>
            <th>销售额</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in data?.topDishes ?? []" :key="r.dishId">
            <td>{{ r.dishName }}</td>
            <td>{{ r.quantity }}</td>
            <td>¥{{ Number(r.salesAmount ?? 0).toFixed(2) }}</td>
          </tr>
          <tr v-if="!loading && (data?.topDishes?.length ?? 0) === 0">
            <td colspan="3" class="empty">暂无数据</td>
          </tr>
          <tr v-if="loading">
            <td colspan="3" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>

      <div v-if="msg" class="msg">{{ msg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface TopDishRow {
  dishId: number
  dishName: string
  quantity: number
  salesAmount: number
}

interface MerchantSalesResponse {
  salesAmount: number
  topDishes: TopDishRow[]
}

const loading = ref(false)
const msg = ref('')
const data = ref<MerchantSalesResponse | null>(null)

const today = new Date()
const end = ref(today.toISOString().slice(0, 10))
const startDate = new Date(today.getTime() - 7 * 24 * 3600 * 1000)
const start = ref(startDate.toISOString().slice(0, 10))

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('currentUser') || '{}')
  } catch {
    return {}
  }
})

const restaurantId = computed<number | null>(() => {
  const v = currentUser.value?.restaurantId
  return typeof v === 'number' ? v : v ? Number(v) : null
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

function go(path: string) {
  router.push(path)
}

async function load() {
  if (!restaurantId.value) return
  msg.value = ''
  loading.value = true
  try {
    const qs = new URLSearchParams()
    qs.set('restaurantId', String(restaurantId.value))
    qs.set('start', start.value)
    qs.set('end', end.value)
    const res = await fetch(api(`/api/merchant/stats/sales?${qs.toString()}`))
    if (!res.ok) {
      msg.value = '查询失败'
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
.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.link {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}

.warn {
  padding: 10px 12px;
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #614700;
  border-radius: 6px;
  margin-top: 12px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: end;
  margin: 12px 0;
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

.summary {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.box {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 10px 12px;
  min-width: 180px;
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
  margin: 8px 0;
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

.msg {
  margin-top: 10px;
  color: #ff4d4f;
  font-size: 13px;
}
</style>
