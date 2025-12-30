<template>
  <div class="card">
    <h2>入驻审核</h2>

    <div class="filters">
      <label>
        状态
        <select v-model="status">
          <option value="">全部</option>
          <option value="PENDING">PENDING</option>
          <option value="APPROVED">APPROVED</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </label>
      <button @click="load">查询</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>餐馆</th>
          <th>账号</th>
          <th>执照号</th>
          <th>联系人</th>
          <th>状态</th>
          <th>备注</th>
          <th>提交时间</th>
          <th>审核时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="a in items" :key="a.id">
          <td>{{ a.id }}</td>
          <td>
            <div class="title">{{ a.restaurantName }}</div>
            <div class="sub">{{ a.address || '-' }} {{ a.phone || '' }}</div>
          </td>
          <td>{{ a.username }}</td>
          <td>{{ a.licenseNo || '-' }}</td>
          <td>{{ a.contactName || '-' }} {{ a.contactPhone || '' }}</td>
          <td>
            <span class="tag" :class="tagClass(a.status)">{{ a.status }}</span>
          </td>
          <td class="remark">{{ a.reviewRemark || '-' }}</td>
          <td>{{ formatTime(a.createdAt) }}</td>
          <td>{{ formatTime(a.reviewedAt) }}</td>
          <td class="actions">
            <button v-if="a.status === 'PENDING'" class="mini primary" @click="openApprove(a)">通过</button>
            <button v-if="a.status === 'PENDING'" class="mini danger" @click="openReject(a)">驳回</button>
            <span v-else class="sub">-</span>
          </td>
        </tr>
        <tr v-if="!loading && items.length === 0">
          <td colspan="10" class="empty">暂无数据</td>
        </tr>
        <tr v-if="loading">
          <td colspan="10" class="empty">加载中...</td>
        </tr>
      </tbody>
    </table>

    <div v-if="message" class="msg">{{ message }}</div>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <div class="dialog">
        <h3>{{ dialog.mode === 'approve' ? '通过申请' : '驳回申请' }} #{{ dialog.item?.id }}</h3>
        <div class="sub">
          {{ dialog.item?.restaurantName }} / 账号：{{ dialog.item?.username }}
        </div>
        <label class="dlg-row">
          备注（可选）
          <textarea v-model="dialog.remark" rows="3" placeholder="审核备注"></textarea>
        </label>
        <div class="dlg-actions">
          <button class="mini" @click="closeDialog">取消</button>
          <button
            class="mini"
            :class="dialog.mode === 'approve' ? 'primary' : 'danger'"
            :disabled="submitting"
            @click="submitDialog"
          >
            {{ submitting ? '处理中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface ApplicationRow {
  id: number
  restaurantName: string
  address: string | null
  phone: string | null
  username: string
  licenseNo: string | null
  contactName: string | null
  contactPhone: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewRemark: string | null
  restaurantId: number | null
  restaurantUserId: number | null
  createdAt: string | null
  reviewedAt: string | null
}

const status = ref<string>('')
const items = ref<ApplicationRow[]>([])
const loading = ref(false)
const message = ref('')

const dialog = ref({
  visible: false,
  mode: 'approve' as 'approve' | 'reject',
  item: null as ApplicationRow | null,
  remark: '',
})

const submitting = ref(false)

function formatTime(v: string | null | undefined) {
  if (!v) return ''
  return v.replace('T', ' ')
}

function tagClass(s: string) {
  if (s === 'APPROVED') return 'ok'
  if (s === 'REJECTED') return 'bad'
  return ''
}

async function load() {
  loading.value = true
  message.value = ''
  try {
    const qs = new URLSearchParams()
    if (status.value) qs.set('status', status.value)
    const res = await fetch(`http://localhost:8081/api/admin/restaurant-applications?${qs.toString()}`)
    if (!res.ok) {
      message.value = '加载失败'
      return
    }
    items.value = (await res.json()) as ApplicationRow[]
  } catch {
    message.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

function openApprove(item: ApplicationRow) {
  dialog.value = { visible: true, mode: 'approve', item, remark: '' }
}

function openReject(item: ApplicationRow) {
  dialog.value = { visible: true, mode: 'reject', item, remark: '' }
}

function closeDialog() {
  dialog.value.visible = false
}

async function submitDialog() {
  if (!dialog.value.item) return
  submitting.value = true
  message.value = ''
  try {
    const url =
      dialog.value.mode === 'approve'
        ? `http://localhost:8081/api/admin/restaurant-applications/${dialog.value.item.id}/approve`
        : `http://localhost:8081/api/admin/restaurant-applications/${dialog.value.item.id}/reject`
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ remark: dialog.value.remark }),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      message.value = (data as any).message ?? '操作失败'
      return
    }
    closeDialog()
    await load()
  } catch {
    message.value = '无法连接到服务器'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void load()
})
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

select {
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
  color: #888;
}

.title {
  font-weight: 600;
}

.sub {
  color: #666;
  font-size: 12px;
  margin-top: 4px;
}

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #d9d9d9;
  font-size: 12px;
  color: #333;
}

.tag.ok {
  border-color: #52c41a;
  color: #52c41a;
}

.tag.bad {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.remark {
  max-width: 220px;
  word-break: break-word;
  white-space: pre-wrap;
}

.actions {
  width: 110px;
}

.mini {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  font-size: 12px;
}

.mini.primary {
  border-color: #1890ff;
  color: #1890ff;
}

.mini.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.msg {
  margin-top: 12px;
  color: #ff4d4f;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.dialog {
  width: 520px;
  background: #fff;
  border-radius: 10px;
  padding: 16px;
}

.dlg-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

textarea {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 8px 10px;
  resize: vertical;
}

.dlg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}
</style>

