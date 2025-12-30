<template>
  <div class="card">
    <h2>骑手账号</h2>

    <div class="form-row">
      <select v-model.number="form.deliveryStaffId">
        <option :value="null">选择骑手</option>
        <option v-for="s in staff" :key="s.id" :value="s.id">{{ s.name }} (#{{ s.id }})</option>
      </select>
      <input v-model="form.username" placeholder="账号" />
      <input v-model="form.password" placeholder="密码" />
      <select v-model="form.status">
        <option value="ACTIVE">启用</option>
        <option value="DISABLED">停用</option>
      </select>
      <button @click="create">创建账号</button>
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
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.deliveryStaffName }} (#{{ u.deliveryStaffId }})</td>
          <td>{{ u.username }}</td>
          <td>{{ u.status }}</td>
          <td class="actions">
            <button class="mini" @click="toggle(u)">{{ u.status === 'ACTIVE' ? '停用' : '启用' }}</button>
            <button class="mini" @click="resetPassword(u)">重置密码</button>
            <button class="mini danger" @click="remove(u.id)">删除</button>
          </td>
        </tr>
        <tr v-if="!loading && users.length === 0">
          <td colspan="5" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="5" class="empty">加载中...</td>
        </tr>
      </tbody>
    </table>

    <div v-if="msg" class="msg">{{ msg }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

interface DeliveryStaff {
  id: number
  name: string
}

interface DeliveryStaffUserRow {
  id: number
  deliveryStaffId: number
  deliveryStaffName: string
  username: string
  status: string
}

const loading = ref(false)
const msg = ref('')
const staff = ref<DeliveryStaff[]>([])
const users = ref<DeliveryStaffUserRow[]>([])

const form = reactive({
  deliveryStaffId: null as number | null,
  username: '',
  password: '',
  status: 'ACTIVE',
})

async function loadAll() {
  loading.value = true
  try {
    const [sRes, uRes] = await Promise.all([
      fetch('http://localhost:8081/api/delivery-staff'),
      fetch('http://localhost:8081/api/delivery-staff-users'),
    ])
    if (sRes.ok) staff.value = await sRes.json()
    if (uRes.ok) users.value = await uRes.json()
  } finally {
    loading.value = false
  }
}

async function create() {
  msg.value = ''
  if (!form.deliveryStaffId || !form.username || !form.password) return
  const res = await fetch('http://localhost:8081/api/delivery-staff-users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deliveryStaffId: form.deliveryStaffId,
      username: form.username,
      password: form.password,
      status: form.status,
    }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    msg.value = data.message ?? '创建失败'
    return
  }
  form.deliveryStaffId = null
  form.username = ''
  form.password = ''
  form.status = 'ACTIVE'
  msg.value = '已创建'
  await loadAll()
}

async function toggle(u: DeliveryStaffUserRow) {
  msg.value = ''
  const next = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const res = await fetch(`http://localhost:8081/api/delivery-staff-users/${u.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: next }),
  })
  if (res.ok) {
    await loadAll()
  }
}

async function resetPassword(u: DeliveryStaffUserRow) {
  msg.value = ''
  const pwd = prompt('输入新密码（不为空）')
  if (!pwd) return
  const res = await fetch(`http://localhost:8081/api/delivery-staff-users/${u.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: pwd }),
  })
  if (res.ok) {
    msg.value = '已重置'
    await loadAll()
  }
}

async function remove(id: number) {
  if (!confirm('确定删除该账号吗？')) return
  const res = await fetch(`http://localhost:8081/api/delivery-staff-users/${id}`, { method: 'DELETE' })
  if (res.ok) {
    msg.value = '已删除'
    await loadAll()
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

.msg {
  margin-top: 10px;
  color: #389e0d;
  font-size: 13px;
}
</style>

