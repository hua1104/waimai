<template>
  <div class="card">
    <h2>食客管理</h2>

    <div class="form-row">
      <input v-model="form.username" placeholder="用户名" :disabled="editingId != null" />
      <input v-model="form.realName" placeholder="姓名 *" />
      <input v-model="form.phone" placeholder="电话 *" />
      <select v-model="form.status">
        <option value="ACTIVE">启用</option>
        <option value="DISABLED">禁用</option>
      </select>
      <input
        v-if="editingId == null"
        v-model="form.password"
        type="password"
        placeholder="初始密码 *（至少6位）"
      />
      <button class="primary" @click="save">
        {{ editingId != null ? '保存修改' : '新增食客' }}
      </button>
      <button v-if="editingId != null" @click="resetForm">取消编辑</button>
      <button class="ghost" @click="loadCustomers">刷新</button>
    </div>

    <p class="tip">可对食客基本信息进行维护，并通过状态控制账号权限（禁用后无法登录）。</p>

    <table class="table">
      <thead>
        <tr>
          <th>编号</th>
          <th>用户名</th>
          <th>姓名</th>
          <th>电话</th>
          <th>状态</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in customers" :key="c.id">
          <td>{{ c.id }}</td>
          <td>{{ c.username }}</td>
          <td>{{ c.realName }}</td>
          <td>{{ c.phone }}</td>
          <td>{{ c.status }}</td>
          <td>{{ formatTime(c.createdAt) }}</td>
          <td class="actions">
            <button @click="startEdit(c)">编辑</button>
            <button class="mini" @click="resetPassword(c.id)">重置密码</button>
            <button v-if="c.status !== 'ACTIVE'" class="mini" @click="setStatus(c.id, 'ACTIVE')">启用</button>
            <button v-if="c.status === 'ACTIVE'" class="mini danger" @click="setStatus(c.id, 'DISABLED')">
              禁用
            </button>
          </td>
        </tr>
        <tr v-if="!loading && customers.length === 0">
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
import { onMounted, ref } from 'vue'

interface Customer {
  id: number
  username: string
  realName: string
  phone: string
  status: string
  createdAt: string | null
}

const customers = ref<Customer[]>([])
const loading = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  username: '',
  password: '',
  realName: '',
  phone: '',
  status: 'ACTIVE',
})

function resetForm() {
  editingId.value = null
  form.value.username = ''
  form.value.password = ''
  form.value.realName = ''
  form.value.phone = ''
  form.value.status = 'ACTIVE'
}

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

async function loadCustomers() {
  loading.value = true
  try {
    const res = await fetch('http://localhost:8081/api/customers')
    if (res.ok) {
      customers.value = await res.json()
    }
  } finally {
    loading.value = false
  }
}

function startEdit(c: Customer) {
  editingId.value = c.id
  form.value.username = c.username
  form.value.password = ''
  form.value.realName = c.realName ?? ''
  form.value.phone = c.phone ?? ''
  form.value.status = c.status ?? 'ACTIVE'
}

async function save() {
  if (!form.value.realName || !form.value.phone) return
  if (editingId.value == null) {
    if (!form.value.username || !form.value.password || form.value.password.length < 6) return
    const res = await fetch('http://localhost:8081/api/customers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.value.username,
        password: form.value.password,
        realName: form.value.realName,
        phone: form.value.phone,
        status: form.value.status,
      }),
    })
    if (!res.ok) return
  } else {
    const res = await fetch(`http://localhost:8081/api/customers/${editingId.value}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.value.username,
        realName: form.value.realName,
        phone: form.value.phone,
        status: form.value.status,
      }),
    })
    if (!res.ok) return
  }
  await loadCustomers()
  resetForm()
}

async function setStatus(id: number, status: string) {
  const res = await fetch(`http://localhost:8081/api/customers/${id}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
  if (res.ok) {
    await loadCustomers()
  }
}

async function resetPassword(id: number) {
  const pwd = prompt('请输入新密码（至少6位）：', '123456')
  if (!pwd) return
  if (pwd.length < 6) return
  const res = await fetch(`http://localhost:8081/api/customers/${id}/password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: pwd }),
  })
  if (!res.ok) return
  alert('已重置')
}

onMounted(loadCustomers)
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
  align-items: center;
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
  border: 1px solid #d9d9d9;
  background-color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.form-row .primary {
  border-color: #1890ff;
  background-color: #1890ff;
  color: #fff;
}

.form-row .ghost {
  border-color: #d9d9d9;
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
  flex-wrap: wrap;
}

.actions .mini,
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
