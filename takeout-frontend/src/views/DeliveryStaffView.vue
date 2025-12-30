<template>
  <div class="card">
    <h2>骑手管理</h2>

    <div class="form-row">
      <input v-model="form.name" placeholder="姓名" />
      <input v-model="form.phone" placeholder="电话" />
      <select v-model="form.status">
        <option value="ACTIVE">启用</option>
        <option value="DISABLED">停用</option>
      </select>
      <input v-model.number="form.currentLoad" type="number" min="0" step="1" placeholder="当前负载" />
      <button @click="save">
        {{ editingId ? '保存修改' : '新增骑手' }}
      </button>
      <button v-if="editingId" class="ghost" @click="resetForm">取消编辑</button>
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
        <tr v-for="s in staff" :key="s.id">
          <td>{{ s.id }}</td>
          <td>{{ s.name }}</td>
          <td>{{ s.phone }}</td>
          <td>{{ s.status }}</td>
          <td>{{ s.currentLoad ?? 0 }}</td>
          <td class="actions">
            <button class="mini" @click="startEdit(s)">编辑</button>
            <button class="mini danger" @click="remove(s.id)">删除</button>
          </td>
        </tr>
        <tr v-if="!loading && staff.length === 0">
          <td colspan="6" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="6" class="empty">加载中...</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

interface DeliveryStaff {
  id: number
  name: string
  phone: string | null
  status: string | null
  currentLoad: number | null
}

const loading = ref(false)
const staff = ref<DeliveryStaff[]>([])
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  phone: '',
  status: 'ACTIVE',
  currentLoad: 0,
})

async function load() {
  loading.value = true
  try {
    const res = await fetch('http://localhost:8081/api/delivery-staff')
    if (res.ok) staff.value = await res.json()
  } finally {
    loading.value = false
  }
}

function startEdit(s: DeliveryStaff) {
  editingId.value = s.id
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

async function save() {
  if (!form.name) return
  const payload = {
    name: form.name,
    phone: form.phone,
    status: form.status,
    currentLoad: form.currentLoad,
  }
  const url =
    editingId.value != null
      ? `http://localhost:8081/api/delivery-staff/${editingId.value}`
      : 'http://localhost:8081/api/delivery-staff'
  const method = editingId.value != null ? 'PUT' : 'POST'

  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (res.ok) {
    await load()
    resetForm()
  }
}

async function remove(id: number) {
  if (!confirm('确定删除该骑手吗？')) return
  const res = await fetch(`http://localhost:8081/api/delivery-staff/${id}`, { method: 'DELETE' })
  if (res.ok) await load()
}

onMounted(load)
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

.ghost {
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
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
</style>

