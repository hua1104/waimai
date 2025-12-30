<template>
  <div class="card">
    <h2>评价管理</h2>

    <div class="tabs">
      <button :class="tab === 'restaurant' ? 'active' : ''" @click="switchTab('restaurant')">饭店评价</button>
      <button :class="tab === 'delivery' ? 'active' : ''" @click="switchTab('delivery')">骑手评价</button>
    </div>

    <div v-if="tab === 'restaurant'" class="panel">
      <div class="filters">
        <label>
          开始日期
          <input v-model="restaurantFilters.start" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="restaurantFilters.end" type="date" />
        </label>
        <label>
          饭店ID
          <input v-model.number="restaurantFilters.restaurantId" type="number" placeholder="可选" />
        </label>
        <label>
          食客ID
          <input v-model.number="restaurantFilters.customerId" type="number" placeholder="可选" />
        </label>
        <button @click="loadRestaurantRatings">查询</button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>订单号</th>
            <th>饭店</th>
            <th>食客</th>
            <th>评分</th>
            <th>内容</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in restaurantRatings" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.orderId }}</td>
            <td>{{ r.restaurantName }} (#{{ r.restaurantId }})</td>
            <td>{{ r.customerUsername }} (#{{ r.customerId }})</td>
            <td>{{ r.score }}</td>
            <td class="content">{{ r.content || '-' }}</td>
            <td>{{ formatTime(r.createdAt) }}</td>
            <td class="actions">
              <button class="mini danger" @click="deleteRestaurantRating(r.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && restaurantRatings.length === 0">
            <td colspan="8" class="empty">暂无数据</td>
          </tr>
          <tr v-if="loading">
            <td colspan="8" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="panel">
      <div class="filters">
        <label>
          开始日期
          <input v-model="deliveryFilters.start" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="deliveryFilters.end" type="date" />
        </label>
        <label>
          骑手ID
          <input v-model.number="deliveryFilters.deliveryStaffId" type="number" placeholder="可选" />
        </label>
        <label>
          食客ID
          <input v-model.number="deliveryFilters.customerId" type="number" placeholder="可选" />
        </label>
        <button @click="loadDeliveryRatings">查询</button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>订单号</th>
            <th>骑手</th>
            <th>食客</th>
            <th>评分</th>
            <th>内容</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in deliveryRatings" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.orderId }}</td>
            <td>{{ r.deliveryStaffName }} (#{{ r.deliveryStaffId }})</td>
            <td>{{ r.customerUsername }} (#{{ r.customerId }})</td>
            <td>{{ r.score }}</td>
            <td class="content">{{ r.content || '-' }}</td>
            <td>{{ formatTime(r.createdAt) }}</td>
            <td class="actions">
              <button class="mini danger" @click="deleteDeliveryRating(r.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && deliveryRatings.length === 0">
            <td colspan="8" class="empty">暂无数据</td>
          </tr>
          <tr v-if="loading">
            <td colspan="8" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="message" class="msg">{{ message }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

type Tab = 'restaurant' | 'delivery'

interface RestaurantRatingRow {
  id: number
  orderId: number
  customerId: number
  customerUsername: string
  restaurantId: number
  restaurantName: string
  score: number
  content: string | null
  createdAt: string
}

interface DeliveryRatingRow {
  id: number
  orderId: number
  customerId: number
  customerUsername: string
  deliveryStaffId: number
  deliveryStaffName: string
  score: number
  content: string | null
  createdAt: string
}

const tab = ref<Tab>('restaurant')
const loading = ref(false)
const message = ref('')

const restaurantRatings = ref<RestaurantRatingRow[]>([])
const deliveryRatings = ref<DeliveryRatingRow[]>([])

const restaurantFilters = ref({
  start: '',
  end: '',
  restaurantId: undefined as number | undefined,
  customerId: undefined as number | undefined,
})

const deliveryFilters = ref({
  start: '',
  end: '',
  deliveryStaffId: undefined as number | undefined,
  customerId: undefined as number | undefined,
})

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

function switchTab(next: Tab) {
  tab.value = next
  message.value = ''
}

async function loadRestaurantRatings() {
  loading.value = true
  message.value = ''
  try {
    const qs = new URLSearchParams()
    if (restaurantFilters.value.start) qs.set('start', restaurantFilters.value.start)
    if (restaurantFilters.value.end) qs.set('end', restaurantFilters.value.end)
    if (restaurantFilters.value.restaurantId != null) qs.set('restaurantId', String(restaurantFilters.value.restaurantId))
    if (restaurantFilters.value.customerId != null) qs.set('customerId', String(restaurantFilters.value.customerId))

    const res = await fetch(`http://localhost:8081/api/admin/ratings/restaurants?${qs.toString()}`)
    if (!res.ok) {
      message.value = '加载饭店评价失败'
      return
    }
    restaurantRatings.value = (await res.json()) as RestaurantRatingRow[]
  } catch {
    message.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

async function loadDeliveryRatings() {
  loading.value = true
  message.value = ''
  try {
    const qs = new URLSearchParams()
    if (deliveryFilters.value.start) qs.set('start', deliveryFilters.value.start)
    if (deliveryFilters.value.end) qs.set('end', deliveryFilters.value.end)
    if (deliveryFilters.value.deliveryStaffId != null) qs.set('deliveryStaffId', String(deliveryFilters.value.deliveryStaffId))
    if (deliveryFilters.value.customerId != null) qs.set('customerId', String(deliveryFilters.value.customerId))

    const res = await fetch(`http://localhost:8081/api/admin/ratings/delivery?${qs.toString()}`)
    if (!res.ok) {
      message.value = '加载骑手评价失败'
      return
    }
    deliveryRatings.value = (await res.json()) as DeliveryRatingRow[]
  } catch {
    message.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

async function deleteRestaurantRating(id: number) {
  if (!confirm('确认要删除这条饭店评价吗？')) return
  message.value = ''
  const res = await fetch(`http://localhost:8081/api/admin/ratings/restaurants/${id}`, { method: 'DELETE' })
  if (!res.ok) {
    message.value = '删除失败'
    return
  }
  await loadRestaurantRatings()
}

async function deleteDeliveryRating(id: number) {
  if (!confirm('确认要删除这条骑手评价吗？')) return
  message.value = ''
  const res = await fetch(`http://localhost:8081/api/admin/ratings/delivery/${id}`, { method: 'DELETE' })
  if (!res.ok) {
    message.value = '删除失败'
    return
  }
  await loadDeliveryRatings()
}

onMounted(() => {
  void loadRestaurantRatings()
})
</script>

<style scoped>
.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}

.tabs button {
  padding: 6px 10px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  border-radius: 4px;
  cursor: pointer;
}

.tabs button.active {
  border-color: #1890ff;
  color: #1890ff;
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
  vertical-align: top;
}

.table thead {
  background-color: #fafafa;
}

.content {
  max-width: 420px;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty {
  text-align: center;
  color: #888;
}

.actions {
  width: 80px;
}

.mini {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
  cursor: pointer;
}

.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.msg {
  margin-top: 12px;
  color: #ff4d4f;
}
</style>

