<template>
  <div class="grid">
    <!-- 左侧：骑手管理 -->
    <div class="card">
      <div class="head">
        <h2>骑手管理</h2>
        <div class="muted">点击骑手行可在右侧管理账号</div>
      </div>

      <div class="form-row">
        <input v-model="form.name" placeholder="姓名" />
        <input v-model="form.phone" placeholder="电话" />
        <select v-model="form.status">
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
        <input v-model.number="form.currentLoad" type="number" min="0" step="1" placeholder="当前负载" />
        <button class="primary" @click="saveStaff">
          {{ editingId ? '保存修改' : '新增骑手' }}
        </button>
        <button v-if="editingId" class="btn" @click="resetForm">取消编辑</button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>姓名</th>
            <th>电话</th>
            <th>状态</th>
            <th>负载</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="s in staff"
            :key="s.id"
            :class="{ selected: selectedStaffId === s.id }"
            @click="selectStaff(s)"
          >
            <td>{{ s.id }}</td>
            <td>{{ s.name }}</td>
            <td>{{ s.phone }}</td>
            <td>{{ s.status }}</td>
            <td>{{ s.currentLoad ?? 0 }}</td>
            <td class="actions" @click.stop>
              <button class="mini" @click="startEdit(s)">编辑</button>
              <button class="mini danger" @click="removeStaff(s.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && staff.length === 0">
            <td colspan="6" class="empty">暂无骑手</td>
          </tr>
          <tr v-if="loading">
            <td colspan="6" class="empty">加载中...</td>
          </tr>
        </tbody>
      </table>

      <div v-if="msg" class="msg">{{ msg }}</div>
    </div>

    <!-- 右侧：骑手账号管理 -->
    <div class="card">
      <div class="head">
        <h2>骑手账号</h2>
        <div class="muted">
          <span v-if="selectedStaff">当前骑手：{{ selectedStaff.name }}（#{{ selectedStaff.id }}）</span>
          <span v-else>请选择左侧一个骑手</span>
        </div>
      </div>

      <div class="form-row">
        <input v-model="userForm.username" placeholder="登录账号" :disabled="!selectedStaffId" />
        <input v-model="userForm.password" placeholder="登录密码" :disabled="!selectedStaffId" />
        <select v-model="userForm.status" :disabled="!selectedStaffId">
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">禁用</option>
        </select>
        <button class="primary" :disabled="!selectedStaffId" @click="createUser">新增账号</button>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>骑手</th>
            <th>账号</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in filteredUsers" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.deliveryStaffName }} (#{{ u.deliveryStaffId }})</td>
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
import { computed, onMounted, reactive, ref } from 'vue'

interface DeliveryStaff {
  id: number
  name: string
  phone: string | null
  status: string | null
  currentLoad: number | null
}

interface DeliveryStaffUserRow {
  id: number
  deliveryStaffId: number
  deliveryStaffName: string
  username: string
  status: string
}

const staff = ref<DeliveryStaff[]>([])
const users = ref<DeliveryStaffUserRow[]>([])
const loading = ref(false)
const msg = ref('')

const editingId = ref<number | null>(null)
const selectedStaffId = ref<number | null>(null)

const form = reactive({
  name: '',
  phone: '',
  status: 'ACTIVE',
  currentLoad: 0,
})

const userForm = reactive({
  username: '',
  password: '',
  status: 'ACTIVE',
})

const selectedStaff = computed(() => staff.value.find((s) => s.id === selectedStaffId.value) ?? null)

const filteredUsers = computed(() => {
  if (!selectedStaffId.value) return users.value
  return users.value.filter((u) => u.deliveryStaffId === selectedStaffId.value)
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

async function loadAll() {
  loading.value = true
  try {
    const [sRes, uRes] = await Promise.all([fetch(api('/api/delivery-staff')), fetch(api('/api/delivery-staff-users'))])
    if (sRes.ok) staff.value = await sRes.json()
    if (uRes.ok) users.value = await uRes.json()
  } finally {
    loading.value = false
  }
}

function selectStaff(s: DeliveryStaff) {
  selectedStaffId.value = s.id
}

function startEdit(s: DeliveryStaff) {
  editingId.value = s.id
  selectedStaffId.value = s.id
  form.name = s.name ?? ''
  form.phone = s.phone ?? ''
  form.status = (s.status as any) || 'ACTIVE'
  form.currentLoad = Number(s.currentLoad ?? 0)
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.phone = ''
  form.status = 'ACTIVE'
  form.currentLoad = 0
}

async function saveStaff() {
  msg.value = ''
  if (!form.name) return
  const payload = {
    name: form.name,
    phone: form.phone,
    status: form.status,
    currentLoad: form.currentLoad,
  }
  const url = editingId.value != null ? api(`/api/delivery-staff/${editingId.value}`) : api('/api/delivery-staff')
  const method = editingId.value != null ? 'PUT' : 'POST'
  const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '保存失败'
    return
  }
  const saved = (await res.json().catch(() => null)) as DeliveryStaff | null
  await loadAll()
  resetForm()
  if (saved?.id) selectedStaffId.value = saved.id
}

async function removeStaff(id: number) {
  if (!confirm('确认删除该骑手？')) return
  const res = await fetch(api(`/api/delivery-staff/${id}`), { method: 'DELETE' })
  if (!res.ok) return
  if (selectedStaffId.value === id) selectedStaffId.value = null
  await loadAll()
}

async function createUser() {
  msg.value = ''
  if (!selectedStaffId.value) return
  if (!userForm.username || !userForm.password) return
  const res = await fetch(api('/api/delivery-staff-users'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deliveryStaffId: selectedStaffId.value,
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

async function toggleUser(u: DeliveryStaffUserRow) {
  const next = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const res = await fetch(api(`/api/delivery-staff-users/${u.id}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: next }),
  })
  if (res.ok) await loadAll()
}

async function resetPassword(u: DeliveryStaffUserRow) {
  const pwd = prompt('请输入新密码')
  if (!pwd) return
  const res = await fetch(api(`/api/delivery-staff-users/${u.id}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: pwd }),
  })
  if (res.ok) await loadAll()
}

async function removeUser(id: number) {
  if (!confirm('确认删除该账号？')) return
  const res = await fetch(api(`/api/delivery-staff-users/${id}`), { method: 'DELETE' })
  if (res.ok) await loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
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

