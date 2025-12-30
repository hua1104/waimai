<template>
  <div class="page">
    <header class="topbar">
      <div class="title">地址簿（食客端）</div>
      <div class="actions">
        <RouterLink class="btn" to="/c">返回点餐</RouterLink>
        <RouterLink class="btn" to="/c/profile">个人信息</RouterLink>
        <RouterLink class="btn" to="/c/orders">我的订单</RouterLink>
        <RouterLink class="btn" to="/c/stats">我的统计</RouterLink>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card">
        <div v-if="!customerId" class="warn">请先登录</div>

        <div v-else>
          <div class="row">
            <input v-model="form.label" placeholder="标签（家/公司，可选）" />
            <input v-model="form.addressDetail" placeholder="详细地址 *" />
            <input v-model="form.contactName" placeholder="联系人（可选）" />
            <input v-model="form.contactPhone" placeholder="联系电话（可选）" />
            <label class="chk">
              <input v-model="form.isDefault" type="checkbox" />
              设为默认
            </label>
            <button class="btn primary" :disabled="!form.addressDetail" @click="save">
              {{ editingId ? '保存' : '新增' }}
            </button>
            <button v-if="editingId" class="btn" @click="reset">取消</button>
            <button class="btn" @click="load">刷新</button>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>默认</th>
                <th>标签</th>
                <th>地址</th>
                <th>联系人</th>
                <th>电话</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in items" :key="a.id">
                <td>{{ a.id }}</td>
                <td>
                  <span v-if="a.isDefault" class="tag ok">默认</span>
                  <button v-else class="mini" @click="setDefault(a.id)">设为默认</button>
                </td>
                <td>{{ a.label || '-' }}</td>
                <td>{{ a.addressDetail }}</td>
                <td>{{ a.contactName || '-' }}</td>
                <td>{{ a.contactPhone || '-' }}</td>
                <td class="actions-cell">
                  <button class="mini" @click="startEdit(a)">编辑</button>
                  <button class="mini danger" @click="remove(a.id)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && items.length === 0">
                <td colspan="7" class="empty">暂无地址</td>
              </tr>
              <tr v-if="loading">
                <td colspan="7" class="empty">加载中...</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="msg" class="msg">{{ msg }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../lib/api'

interface AddressRow {
  id: number
  label: string | null
  addressDetail: string
  contactName: string | null
  contactPhone: string | null
  isDefault: boolean
}

const router = useRouter()
const loading = ref(false)
const msg = ref('')
const items = ref<AddressRow[]>([])
const editingId = ref<number | null>(null)

const customerId = computed<number | null>(() => {
  try {
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const v = user?.userId
    return typeof v === 'number' ? v : v ? Number(v) : null
  } catch {
    return null
  }
})

const form = reactive({
  label: '',
  addressDetail: '',
  contactName: '',
  contactPhone: '',
  isDefault: false,
})

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

function reset() {
  editingId.value = null
  form.label = ''
  form.addressDetail = ''
  form.contactName = ''
  form.contactPhone = ''
  form.isDefault = false
}

function startEdit(a: AddressRow) {
  editingId.value = a.id
  form.label = a.label ?? ''
  form.addressDetail = a.addressDetail ?? ''
  form.contactName = a.contactName ?? ''
  form.contactPhone = a.contactPhone ?? ''
  form.isDefault = a.isDefault
}

async function load() {
  if (!customerId.value) return
  loading.value = true
  msg.value = ''
  try {
    const res = await fetch(api(`/api/customer/addresses?customerId=${customerId.value}`))
    if (!res.ok) {
      msg.value = '加载失败'
      return
    }
    const raw = (await res.json()) as any[]
    items.value = raw.map((a) => ({
      ...a,
      isDefault: typeof a?.isDefault === 'boolean' ? a.isDefault : !!a?.default,
    })) as AddressRow[]
  } catch {
    msg.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!customerId.value) return
  msg.value = ''

  const payload = {
    customerId: customerId.value,
    label: form.label || null,
    addressDetail: form.addressDetail,
    contactName: form.contactName || null,
    contactPhone: form.contactPhone || null,
    isDefault: form.isDefault,
  }

  const url = editingId.value ? api(`/api/customer/addresses/${editingId.value}`) : api('/api/customer/addresses')
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

async function setDefault(id: number) {
  if (!customerId.value) return
  msg.value = ''
  const res = await fetch(api(`/api/customer/addresses/${id}/default?customerId=${customerId.value}`), { method: 'POST' })
  if (!res.ok) {
    msg.value = '设置默认失败'
    return
  }
  await load()
}

async function remove(id: number) {
  if (!customerId.value) return
  msg.value = ''
  if (!confirm('确定删除该地址？')) return
  const res = await fetch(api(`/api/customer/addresses/${id}?customerId=${customerId.value}`), { method: 'DELETE' })
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
  background: var(--bg);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.78);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  gap: 12px;
}

.title {
  font-weight: 900;
  letter-spacing: 0.5px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.content {
  padding: 18px 16px 28px;
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  padding: 12px;
}

.warn {
  color: var(--danger);
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

input {
  min-width: 160px;
}

.chk {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 13px;
  color: rgba(15, 23, 42, 0.8);
}

.actions-cell {
  width: 140px;
}

.empty {
  text-align: center;
  color: var(--muted);
}

.msg {
  margin-top: 10px;
  color: var(--danger);
}
</style>
