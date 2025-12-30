<template>
  <div class="grid">
    <!-- 左侧：饭店管理 -->
    <div class="card">
      <div class="head">
        <h2>饭店管理</h2>
        <div class="muted">点击饭店行可在右侧管理账号</div>
      </div>

      <div class="form-row">
        <input v-model="form.name" placeholder="饭店名称" />
        <input v-model="form.address" placeholder="地址" />
        <input v-model="form.phone" placeholder="电话" />
        <select v-model="form.status">
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">禁用</option>
          <option value="PENDING">待审核</option>
        </select>

        <div class="inline-row">
          <label class="inline">
            <input v-model="useDefaultCommission" type="checkbox" />
            使用平台默认抽成（{{ platformDefaultRate }}%）
          </label>
          <button v-if="useDefaultCommission" class="mini" type="button" @click="enableCustomCommission">自定义</button>
        </div>
        <input
          v-model="commissionRateText"
          ref="commissionInput"
          type="number"
          min="0"
          max="100"
          step="0.1"
          placeholder="抽成比例(%)"
          :disabled="useDefaultCommission"
        />

        <button class="primary" @click="saveRestaurant">
          {{ editingId ? '保存修改' : '新增饭店' }}
        </button>
        <button v-if="editingId" class="btn" @click="resetForm">取消编辑</button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>地址</th>
            <th>电话</th>
            <th>状态</th>
            <th>抽成(%)</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in restaurants"
            :key="r.id"
            :class="{ selected: selectedRestaurantId === r.id }"
            @click="startEdit(r)"
          >
            <td>{{ r.id }}</td>
            <td>{{ r.name }}</td>
            <td>{{ r.address }}</td>
            <td>{{ r.phone }}</td>
            <td>{{ r.status }}</td>
            <td>
              {{ r.commissionRate ?? platformDefaultRate }}
              <span v-if="r.commissionRate == null" class="muted">(默认)</span>
            </td>
            <td class="actions" @click.stop>
              <button class="mini" @click="startEdit(r)">编辑</button>
              <button class="mini danger" @click="removeRestaurant(r.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && restaurants.length === 0">
            <td colspan="7" class="empty">暂无饭店</td>
          </tr>
          <tr v-if="loading">
            <td colspan="7" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>

      <div v-if="msg" class="msg">{{ msg }}</div>
    </div>

    <!-- 右侧：饭店账号管理 -->
    <div class="card">
      <div class="head">
        <h2>饭店账号</h2>
        <div class="muted">
          <span v-if="selectedRestaurant">当前饭店：{{ selectedRestaurant.name }}（#{{ selectedRestaurant.id }}）</span>
          <span v-else>请选择左侧一个饭店</span>
        </div>
      </div>

      <div class="form-row">
        <input v-model="userForm.username" placeholder="登录账号" :disabled="!selectedRestaurantId" />
        <input v-model="userForm.password" placeholder="登录密码" :disabled="!selectedRestaurantId" />
        <select v-model="userForm.status" :disabled="!selectedRestaurantId">
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
        <button class="primary" :disabled="!selectedRestaurantId" @click="createUser">新增账号</button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>饭店</th>
            <th>账号</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in filteredUsers" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.restaurantName }} (#{{ u.restaurantId }})</td>
            <td>{{ u.username }}</td>
            <td>{{ u.status }}</td>
            <td class="actions">
              <button class="mini" @click="toggleUser(u)">{{ u.status === 'ACTIVE' ? '禁用' : '启用' }}</button>
              <button class="mini" @click="resetPassword(u)">重置密码</button>
              <button class="mini danger" @click="removeUser(u.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && filteredUsers.length === 0">
            <td colspan="5" class="empty">暂无账号</td>
          </tr>
          <tr v-if="loading">
            <td colspan="5" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'

interface Restaurant {
  id: number
  name: string
  address: string
  phone: string
  status: string
  commissionRate: number | null
}

interface RestaurantUserRow {
  id: number
  restaurantId: number
  restaurantName: string
  username: string
  status: string
}

const restaurants = ref<Restaurant[]>([])
const users = ref<RestaurantUserRow[]>([])
const loading = ref(false)
const msg = ref('')

const editingId = ref<number | null>(null)
const selectedRestaurantId = ref<number | null>(null)

const platformDefaultRate = ref<number>(0)
const useDefaultCommission = ref(false)
const commissionRateText = ref('0')
const commissionInput = ref<HTMLInputElement | null>(null)

function enableCustomCommission() {
  useDefaultCommission.value = false
}

watch(useDefaultCommission, (v) => {
  if (!v) nextTick(() => commissionInput.value?.focus())
})

const form = reactive({
  name: '',
  address: '',
  phone: '',
  status: 'ACTIVE',
  commissionRate: null as number | null,
})

const userForm = reactive({
  username: '',
  password: '',
  status: 'ACTIVE',
})

const selectedRestaurant = computed(() => restaurants.value.find((r) => r.id === selectedRestaurantId.value) ?? null)

const filteredUsers = computed(() => {
  if (!selectedRestaurantId.value) return users.value
  return users.value.filter((u) => u.restaurantId === selectedRestaurantId.value)
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

async function loadPlatformCommission() {
  const res = await fetch(api('/api/admin/config/commission'))
  if (!res.ok) return
  const data = await res.json()
  platformDefaultRate.value = Number(data.defaultCommissionRate ?? 0)
}

async function loadAll() {
  loading.value = true
  try {
    const [rRes, uRes] = await Promise.all([fetch(api('/api/restaurants')), fetch(api('/api/restaurant-users'))])
    if (rRes.ok) restaurants.value = await rRes.json()
    if (uRes.ok) users.value = await uRes.json()
  } finally {
    loading.value = false
  }
}

function startEdit(r: Restaurant) {
  editingId.value = r.id
  selectedRestaurantId.value = r.id
  form.name = r.name
  form.address = r.address
  form.phone = r.phone
  form.status = r.status
  form.commissionRate = r.commissionRate == null ? null : Number(r.commissionRate)
  useDefaultCommission.value = r.commissionRate == null
  commissionRateText.value = r.commissionRate == null ? String(platformDefaultRate.value) : String(r.commissionRate)
  msg.value = ''
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
  msg.value = ''
  if (!form.name) return

  let commissionRate: number | null = null
  if (!useDefaultCommission.value) {
    const t = String(commissionRateText.value ?? '').trim()
    if (t) {
      const n = Number(t)
      if (!Number.isFinite(n) || n < 0 || n > 100) return
      commissionRate = n
    }
  }

  const payload = {
    name: form.name,
    address: form.address,
    phone: form.phone,
    status: form.status,
    commissionRate,
  }

  const url = editingId.value != null ? api(`/api/restaurants/${editingId.value}`) : api('/api/restaurants')
  const method = editingId.value != null ? 'PUT' : 'POST'

  const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '保存失败'
    return
  }

  const saved = (await res.json().catch(() => null)) as Restaurant | null
  await loadAll()
  resetForm()
  if (saved?.id) selectedRestaurantId.value = saved.id
}

async function removeRestaurant(id: number) {
  if (!confirm('确认删除该饭店（将级联删除相关数据）？')) return
  const res = await fetch(api(`/api/restaurants/${id}`), { method: 'DELETE' })
  if (!res.ok) return
  if (selectedRestaurantId.value === id) selectedRestaurantId.value = null
  await loadAll()
}

async function createUser() {
  msg.value = ''
  if (!selectedRestaurantId.value) return
  if (!userForm.username || !userForm.password) return

  const res = await fetch(api('/api/restaurant-users'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      restaurantId: selectedRestaurantId.value,
      username: userForm.username,
      password: userForm.password,
      status: userForm.status,
    }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '新增账号失败'
    return
  }
  userForm.username = ''
  userForm.password = ''
  userForm.status = 'ACTIVE'
  await loadAll()
}

async function toggleUser(u: RestaurantUserRow) {
  const next = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const res = await fetch(api(`/api/restaurant-users/${u.id}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: next }),
  })
  if (res.ok) await loadAll()
}

async function resetPassword(u: RestaurantUserRow) {
  const pwd = prompt('请输入新密码')
  if (!pwd) return
  const res = await fetch(api(`/api/restaurant-users/${u.id}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: pwd }),
  })
  if (res.ok) await loadAll()
}

async function removeUser(id: number) {
  if (!confirm('确认删除该账号？')) return
  const res = await fetch(api(`/api/restaurant-users/${id}`), { method: 'DELETE' })
  if (res.ok) await loadAll()
}

onMounted(async () => {
  await loadPlatformCommission()
  resetForm()
  await loadAll()
})
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 6px;
}

.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

input,
select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  font-size: 13px;
}

.inline {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #333;
}

.inline-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.primary {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #1890ff;
  background-color: #1890ff;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.btn {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
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

.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mini {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #333;
}

.mini.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.muted {
  color: #999;
  font-size: 12px;
}

.msg {
  margin-top: 10px;
  color: #389e0d;
  font-size: 13px;
}

tr.selected {
  background: #e6f7ff;
}
</style>
