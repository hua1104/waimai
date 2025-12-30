<template>
  <div class="card">
    <h2>平台统计</h2>

    <div class="filters">
      <label>
        开始日期
        <input v-model="start" type="date" />
      </label>
      <label>
        结束日期
        <input v-model="end" type="date" />
      </label>
      <button @click="loadAll">查询</button>
    </div>

    <div class="summary">
      <div class="summary-item">
        <div class="k">平台收入（抽成）</div>
        <div class="v">¥{{ Number(platform?.platformIncome ?? 0).toFixed(2) }}</div>
      </div>
      <div class="summary-item">
        <div class="k">平台总销售额</div>
        <div class="v">¥{{ Number(platform?.totalSales ?? 0).toFixed(2) }}</div>
      </div>
    </div>

    <div class="grid2">
      <section>
        <h3 class="sub">销量饭店 TOP</h3>
        <table class="table">
          <thead>
            <tr>
              <th>饭店ID</th>
              <th>饭店名称</th>
              <th>销售额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in platform?.topRestaurants ?? []" :key="r.restaurantId">
              <td>{{ r.restaurantId }}</td>
              <td>{{ r.restaurantName }}</td>
              <td>¥{{ Number(r.salesAmount ?? 0).toFixed(2) }}</td>
            </tr>
            <tr v-if="!loading && (platform?.topRestaurants?.length ?? 0) === 0">
              <td colspan="3" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h3 class="sub">热销菜品 TOP</h3>
        <table class="table">
          <thead>
            <tr>
              <th>菜品ID</th>
              <th>菜品名称</th>
              <th>销量</th>
              <th>销售额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in dishes?.topDishes ?? []" :key="d.dishId">
              <td>{{ d.dishId }}</td>
              <td>{{ d.dishName }}</td>
              <td>{{ d.quantity }}</td>
              <td>¥{{ Number(d.salesAmount ?? 0).toFixed(2) }}</td>
            </tr>
            <tr v-if="!loading && (dishes?.topDishes?.length ?? 0) === 0">
              <td colspan="4" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>

    <div class="grid2">
      <section>
        <h3 class="sub">评分饭店 TOP</h3>
        <table class="table">
          <thead>
            <tr>
              <th>饭店ID</th>
              <th>饭店名称</th>
              <th>平均分</th>
              <th>评价数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in ratings?.topRestaurantsByRating ?? []" :key="r.restaurantId">
              <td>{{ r.restaurantId }}</td>
              <td>{{ r.restaurantName }}</td>
              <td>{{ Number(r.avgScore ?? 0).toFixed(2) }}</td>
              <td>{{ r.ratingCount }}</td>
            </tr>
            <tr v-if="!loading && (ratings?.topRestaurantsByRating?.length ?? 0) === 0">
              <td colspan="4" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h3 class="sub">评分骑手 TOP</h3>
        <table class="table">
          <thead>
            <tr>
              <th>骑手ID</th>
              <th>骑手姓名</th>
              <th>平均分</th>
              <th>评价数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in ratings?.topRidersByRating ?? []" :key="r.deliveryStaffId">
              <td>{{ r.deliveryStaffId }}</td>
              <td>{{ r.deliveryStaffName }}</td>
              <td>{{ Number(r.avgScore ?? 0).toFixed(2) }}</td>
              <td>{{ r.ratingCount }}</td>
            </tr>
            <tr v-if="!loading && (ratings?.topRidersByRating?.length ?? 0) === 0">
              <td colspan="4" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>

    <div v-if="loading" class="empty">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface RestaurantRow {
  restaurantId: number
  restaurantName: string
  salesAmount: number
}

interface PlatformStats {
  totalSales: number
  platformIncome: number
  topRestaurants: RestaurantRow[]
}

interface TopDishRow {
  dishId: number
  dishName: string
  quantity: number
  salesAmount: number
}

interface TopDishesResponse {
  topDishes: TopDishRow[]
}

interface RestaurantRatingRankRow {
  restaurantId: number
  restaurantName: string
  avgScore: number
  ratingCount: number
}

interface RiderRatingRankRow {
  deliveryStaffId: number
  deliveryStaffName: string
  avgScore: number
  ratingCount: number
}

interface RatingsRankResponse {
  topRestaurantsByRating: RestaurantRatingRankRow[]
  topRidersByRating: RiderRatingRankRow[]
}

const today = new Date()
const yyyy = today.getFullYear()
const mm = String(today.getMonth() + 1).padStart(2, '0')
const dd = String(today.getDate()).padStart(2, '0')

const start = ref(`${yyyy}-${mm}-01`)
const end = ref(`${yyyy}-${mm}-${dd}`)

const loading = ref(false)
const platform = ref<PlatformStats | null>(null)
const dishes = ref<TopDishesResponse | null>(null)
const ratings = ref<RatingsRankResponse | null>(null)

async function loadAll() {
  loading.value = true
  try {
    const [pRes, dRes, rRes] = await Promise.all([
      fetch(`http://localhost:8081/api/stats/platform?start=${start.value}&end=${end.value}`),
      fetch(`http://localhost:8081/api/stats/dishes?start=${start.value}&end=${end.value}`),
      fetch(`http://localhost:8081/api/stats/ratings?start=${start.value}&end=${end.value}`),
    ])
    if (pRes.ok) platform.value = await pRes.json()
    if (dRes.ok) dishes.value = await dRes.json()
    if (rRes.ok) ratings.value = await rRes.json()
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
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
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 12px;
  margin: 12px 0 16px;
}

.summary-item {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.k {
  color: #666;
  font-size: 12px;
}

.v {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 600;
}

.sub {
  margin: 8px 0;
}

.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 12px;
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
}
</style>

