<template>
  <div class="page">
    <div class="card">
      <h2>优惠活动（饭店端）</h2>
      <div class="sub">当前仅支持“满减”：满足门槛后自动减免（下单时按最大减免一条计算）。</div>

      <div v-if="!restaurantId" class="warn">当前账号缺少 restaurantId，请重新用“饭店”角色登录。</div>

      <div v-else>
        <div class="row">
          <input v-model.number="form.thresholdAmount" type="number" step="0.01" placeholder="门槛金额(满)" />
          <input v-model.number="form.discountAmount" type="number" step="0.01" placeholder="减免金额(减)" />
          <input v-model="form.start" type="date" />
          <input v-model="form.end" type="date" />
          <select v-model="form.status">
            <option value="ACTIVE">ACTIVE</option>
            <option value="DISABLED">DISABLED</option>
          </select>
          <button class="btn primary" :disabled="!canSave" @click="save">
            {{ editingId ? '保存修改' : '新增满减' }}
          </button>
          <button v-if="editingId" class="btn" @click="reset">取消编辑</button>
          <button class="btn" @click="load">刷新</button>
        </div>
        <div v-if="validationMsg" class="warn">{{ validationMsg }}</div>

        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>类型</th>
              <th>门槛</th>
              <th>减免</th>
              <th>有效期</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in items" :key="p.id">
              <td>{{ p.id }}</td>
              <td>{{ p.type }}</td>
              <td>满 {{ p.thresholdAmount ?? 0 }}</td>
              <td>减 {{ p.discountAmount ?? 0 }}</td>
              <td>
                <div class="sub">{{ shortDate(p.startAt) || '不限' }} ~ {{ shortDate(p.endAt) || '不限' }}</div>
              </td>
              <td>{{ p.status }}</td>
              <td class="sub">{{ formatTime(p.createdAt) }}</td>
              <td class="actions">
                <button class="btn" @click="startEdit(p)">编辑</button>
                <button class="btn danger" @click="remove(p.id)">删除</button>
              </td>
            </tr>
            <tr v-if="!loading && items.length === 0">
              <td colspan="8" class="empty">暂无活动</td>
            </tr>
            <tr v-if="loading">
              <td colspan="8" class="empty">加载中...</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="msg" class="msg">{{ msg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

interface PromotionRow {
  id: number
  type: string
  thresholdAmount: number
  discountAmount: number
  status: string
  startAt: string | null
  endAt: string | null
  createdAt: string | null
}

const loading = ref(false)
const msg = ref('')

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

const items = ref<PromotionRow[]>([])
const editingId = ref<number | null>(null)

const form = reactive({
  thresholdAmount: null as number | null,
  discountAmount: null as number | null,
  start: '' as string,
  end: '' as string,
  status: 'ACTIVE' as string,
})

const validationMsg = computed(() => {
  const threshold = Number(form.thresholdAmount ?? 0)
  const discount = Number(form.discountAmount ?? 0)
  if (!Number.isFinite(threshold) || threshold <= 0) return '请输入门槛金额（>0）'
  if (!Number.isFinite(discount) || discount <= 0) return '请输入减免金额（>0）'
  if (discount > threshold) return '减免金额不能大于门槛金额'
  if (form.start && form.end && form.end < form.start) return '结束日期不能早于开始日期'
  return ''
})

const canSave = computed(() => !validationMsg.value)

function api(path: string) {
  return `http://localhost:8081${path}`
}

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

function shortDate(v: string | null | undefined) {
  if (!v) return ''
  return v.slice(0, 10)
}

function reset() {
  editingId.value = null
  form.thresholdAmount = null
  form.discountAmount = null
  form.start = ''
  form.end = ''
  form.status = 'ACTIVE'
}

async function load() {
  if (!restaurantId.value) return
  loading.value = true
  msg.value = ''
  try {
    const res = await fetch(api(`/api/merchant/${restaurantId.value}/promotions`))
    if (!res.ok) {
      msg.value = '加载失败'
      return
    }
    items.value = (await res.json()) as PromotionRow[]
  } catch {
    msg.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

function startEdit(p: PromotionRow) {
  editingId.value = p.id
  form.thresholdAmount = Number(p.thresholdAmount ?? 0)
  form.discountAmount = Number(p.discountAmount ?? 0)
  form.start = shortDate(p.startAt) || ''
  form.end = shortDate(p.endAt) || ''
  form.status = p.status || 'ACTIVE'
}

async function save() {
  if (!restaurantId.value) return
  msg.value = ''
  const payload = {
    thresholdAmount: form.thresholdAmount,
    discountAmount: form.discountAmount,
    status: form.status,
    start: form.start || null,
    end: form.end || null,
  }

  const url = editingId.value
    ? api(`/api/merchant/${restaurantId.value}/promotions/${editingId.value}`)
    : api(`/api/merchant/${restaurantId.value}/promotions`)
  const method = editingId.value ? 'PUT' : 'POST'

  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    msg.value = (data as any).message ?? '保存失败'
    return
  }
  reset()
  await load()
}

async function remove(id: number) {
  if (!restaurantId.value) return
  msg.value = ''
  if (!confirm('确定删除该优惠活动？')) return
  const res = await fetch(api(`/api/merchant/${restaurantId.value}/promotions/${id}`), { method: 'DELETE' })
  if (!res.ok) {
    msg.value = '删除失败'
    return
  }
  await load()
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.sub {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
}

.warn {
  margin-top: 12px;
  color: #ff4d4f;
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
  align-items: end;
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
  background-color: #fff;
  color: #333;
  cursor: pointer;
  font-size: 13px;
}

.btn.primary {
  border-color: #1890ff;
  color: #1890ff;
}

.btn.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
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

.actions {
  width: 120px;
}

.empty {
  text-align: center;
  color: #888;
}

.msg {
  margin-top: 12px;
  color: #ff4d4f;
}
</style>
