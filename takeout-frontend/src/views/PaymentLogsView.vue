<template>
  <div class="card">
    <h2>支付/退款流水</h2>

    <div class="filters">
      <label>
        订单号
        <input v-model="filters.orderId" placeholder="可选" />
      </label>
      <label>
        类型
        <select v-model="filters.type">
          <option value="">全部</option>
          <option value="PAY">PAY</option>
          <option value="REFUND">REFUND</option>
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
      <button @click="load(1)">查询</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>订单号</th>
          <th>类型</th>
          <th>金额</th>
          <th>方式</th>
          <th>操作者</th>
          <th>状态</th>
          <th>时间</th>
          <th>备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rows" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.orderId }}</td>
          <td>
            <span class="tag" :class="r.type === 'REFUND' ? 'warn' : 'ok'">{{ r.type }}</span>
          </td>
          <td>{{ Number(r.amount ?? 0).toFixed(2) }}</td>
          <td>{{ r.method }}</td>
          <td>{{ r.operatorRole }}#{{ r.operatorId ?? '-' }}</td>
          <td>{{ r.status }}</td>
          <td>{{ formatTime(r.createdAt) }}</td>
          <td>{{ r.note ?? '' }}</td>
        </tr>
        <tr v-if="!loading && rows.length === 0">
          <td colspan="9" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="9" class="empty">加载中...</td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <button :disabled="page <= 1" @click="load(page - 1)">上一页</button>
      <span>第 {{ page }} 页，共 {{ total }} 条</span>
      <button :disabled="page * size >= total" @click="load(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface PaymentLogRow {
  id: number
  orderId: number
  type: string
  amount: number
  method: string
  operatorRole: string | null
  operatorId: number | null
  status: string
  note: string | null
  createdAt: string
}

interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

const loading = ref(false)
const rows = ref<PaymentLogRow[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const filters = ref({
  orderId: '',
  type: '',
  start: '',
  end: '',
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

async function load(targetPage: number) {
  loading.value = true
  try {
    const qs = new URLSearchParams()
    qs.set('page', String(targetPage))
    qs.set('size', String(size.value))
    if (filters.value.orderId) qs.set('orderId', filters.value.orderId)
    if (filters.value.type) qs.set('type', filters.value.type)
    if (filters.value.start) qs.set('start', filters.value.start)
    if (filters.value.end) qs.set('end', filters.value.end)

    const res = await fetch(api(`/api/admin/payment-logs?${qs.toString()}`))
    if (!res.ok) return
    const data = (await res.json()) as PageResponse<PaymentLogRow>
    rows.value = data.items
    page.value = data.page
    size.value = data.size
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: end;
  margin-bottom: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

input,
select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  min-width: 140px;
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

.pager button {
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  font-size: 12px;
  color: #666;
  background: #fff;
}

.tag.ok {
  border-color: #b7eb8f;
  color: #389e0d;
}

.tag.warn {
  border-color: #ffe58f;
  color: #d48806;
}
</style>

