<template>
  <div class="card">
    <h2>饭店管理</h2>

    <div class="form-row">
      <input v-model="form.name" placeholder="饭店名称" />
      <input v-model="form.address" placeholder="地址" />
      <input v-model="form.phone" placeholder="电话" />
      <select v-model="form.status">
        <option value="ACTIVE">启用</option>
        <option value="DISABLED">停用</option>
      </select>
      <label class="inline">
        <input v-model="useDefaultCommission" type="checkbox" />
        使用平台默认({{ platformDefaultRate }}%)
      </label>
      <input
        v-model="commissionRateText"
        type="number"
        min="0"
        max="100"
        step="0.1"
        placeholder="抽成比例%"
        :disabled="useDefaultCommission"
      />
      <button @click="saveRestaurant">
        {{ editingId ? '保存修改' : '新增饭店' }}
      </button>
      <button v-if="editingId" @click="resetForm">取消编辑</button>
    </div>

    <p class="tip">平台默认抽成：{{ platformDefaultRate }}%，饭店勾选“使用平台默认”时会按平台默认抽成。</p>

    <table class="table">
      <thead>
        <tr>
          <th>编号</th>
          <th>名称</th>
          <th>地址</th>
          <th>电话</th>
          <th>状态</th>
          <th>抽成比例(%)</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in activeRestaurants" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.name }}</td>
          <td>{{ r.address }}</td>
          <td>{{ r.phone }}</td>
          <td>{{ r.status }}</td>
          <td>
            {{ r.commissionRate ?? platformDefaultRate }}
            <span v-if="r.commissionRate == null" class="muted">(默认)</span>
          </td>
          <td class="actions">
            <button @click="startEdit(r)">编辑</button>
            <button class="danger" @click="removeRestaurant(r.id)">删除</button>
          </td>
        </tr>
        <tr v-if="!loading && activeRestaurants.length === 0">
          <td colspan="7" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="7" class="empty">加载中...</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

interface Restaurant {
  id: number
  name: string
  address: string
  phone: string
  status: string
  commissionRate: number | null
}

const restaurants = ref<Restaurant[]>([])
const loading = ref(false)
const editingId = ref<number | null>(null)

const platformDefaultRate = ref<number>(0)
const useDefaultCommission = ref(false)
const commissionRateText = ref('0')

const activeRestaurants = computed(() =>
  restaurants.value,
)

const form = reactive({
  name: '',
  address: '',
  phone: '',
  status: 'ACTIVE',
  commissionRate: null as number | null,
})

async function loadRestaurants() {
  loading.value = true
  try {
    const res = await fetch('http://localhost:8081/api/restaurants')
    if (res.ok) {
      restaurants.value = await res.json()
    }
  } finally {
    loading.value = false
  }
}

async function loadPlatformCommission() {
  const res = await fetch('http://localhost:8081/api/admin/config/commission')
  if (!res.ok) return
  const data = await res.json()
  platformDefaultRate.value = Number(data.defaultCommissionRate ?? 0)
}

function startEdit(r: Restaurant) {
  editingId.value = r.id
  form.name = r.name
  form.address = r.address
  form.phone = r.phone
  form.status = r.status
  form.commissionRate = r.commissionRate == null ? null : Number(r.commissionRate)
  useDefaultCommission.value = r.commissionRate == null
  commissionRateText.value = r.commissionRate == null ? String(platformDefaultRate.value) : String(r.commissionRate)
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.address = ''
  form.phone = ''
  form.status = 'ACTIVE'
  form.commissionRate = null
  useDefaultCommission.value = false
  commissionRateText.value = String(platformDefaultRate.value)
}

async function saveRestaurant() {
  if (!form.name) return

  let commissionRate: number | null = null
  if (!useDefaultCommission.value) {
    const t = commissionRateText.value.trim()
    if (t) {
      const n = Number(t)
      if (!Number.isFinite(n) || n < 0 || n > 100) return
      commissionRate = n
    } else {
      commissionRate = null
    }
  }

  const payload = {
    name: form.name,
    address: form.address,
    phone: form.phone,
    status: form.status,
    commissionRate,
  }

  const url =
    editingId.value != null
      ? `http://localhost:8081/api/restaurants/${editingId.value}`
      : 'http://localhost:8081/api/restaurants'

  const method = editingId.value != null ? 'PUT' : 'POST'

  const res = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (res.ok) {
    await loadRestaurants()
    resetForm()
  }
}

async function removeRestaurant(id: number) {
  if (!confirm('确定要删除该饭店吗？')) return
  const res = await fetch(`http://localhost:8081/api/restaurants/${id}`, {
    method: 'DELETE',
  })
  if (res.ok) {
    await loadRestaurants()
  }
}

onMounted(loadRestaurants)
onMounted(() => {
  void loadPlatformCommission().then(() => resetForm())
})
</script>

<style scoped>
.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.form-row input,
.form-row select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  font-size: 13px;
}

.form-row button {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #1890ff;
  background-color: #1890ff;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.inline {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #333;
}

.muted {
  color: #999;
  margin-left: 6px;
  font-size: 12px;
}

.tip {
  margin-bottom: 12px;
  color: #666;
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
}

.actions button {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.actions .danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}
</style>
