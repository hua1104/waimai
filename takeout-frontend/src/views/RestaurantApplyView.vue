<template>
  <div class="auth">
    <div class="auth-card">
      <div class="head">
        <h2>饭店入驻申请</h2>
        <div class="sub">提交后等待管理员审核，通过后即可用“饭店”角色登录。</div>
      </div>

      <form class="form" @submit.prevent="submit">
        <div class="group">
          <div class="group-title">餐馆信息</div>
          <label class="field">
            <span>餐馆名称 *</span>
            <input v-model="form.restaurantName" required placeholder="如：幸福小馆" />
          </label>
          <label class="field">
            <span>地址</span>
            <input v-model="form.address" placeholder="可选" />
          </label>
          <label class="field">
            <span>电话</span>
            <input v-model="form.phone" placeholder="可选" />
          </label>
          <label class="field">
            <span>营业执照号</span>
            <input v-model="form.licenseNo" placeholder="可选" />
          </label>
          <label class="field">
            <span>联系人</span>
            <input v-model="form.contactName" placeholder="可选" />
          </label>
          <label class="field">
            <span>联系人电话</span>
            <input v-model="form.contactPhone" placeholder="可选" />
          </label>
        </div>

        <div class="group">
          <div class="group-title">登录账号</div>
          <label class="field">
            <span>登录用户名 *</span>
            <input v-model="form.username" required autocomplete="username" placeholder="用于饭店端登录" />
          </label>
          <label class="field">
            <span>登录密码 *</span>
            <input v-model="form.password" type="password" required autocomplete="new-password" placeholder="至少 6 位" />
          </label>

          <div class="btns">
            <button class="primary" type="submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交申请' }}
            </button>
            <button class="ghost" type="button" @click="checkStatus" :disabled="!form.username">
              查询该用户名申请状态
            </button>
            <RouterLink class="ghost link" to="/login">返回登录</RouterLink>
          </div>
        </div>
      </form>

      <div v-if="statusRow" class="status">
        <div><b>申请ID</b>：{{ statusRow.id }}</div>
        <div><b>餐馆</b>：{{ statusRow.restaurantName }}</div>
        <div><b>状态</b>：{{ statusRow.status }}</div>
        <div v-if="statusRow.reviewRemark"><b>备注</b>：{{ statusRow.reviewRemark }}</div>
        <div v-if="statusRow.createdAt"><b>提交时间</b>：{{ formatTime(statusRow.createdAt) }}</div>
        <div v-if="statusRow.reviewedAt"><b>审核时间</b>：{{ formatTime(statusRow.reviewedAt) }}</div>
        <div v-if="statusRow.status === 'APPROVED' && statusRow.restaurantId">
          <b>饭店ID</b>：{{ statusRow.restaurantId }}
        </div>
      </div>

      <div v-if="message" class="msg">{{ message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

const form = reactive({
  restaurantName: '',
  address: '',
  phone: '',
  licenseNo: '',
  contactName: '',
  contactPhone: '',
  username: '',
  password: '',
})

const submitting = ref(false)
const message = ref('')

interface StatusRow {
  id: number
  restaurantName: string
  status: string
  reviewRemark: string | null
  createdAt: string | null
  reviewedAt: string | null
  restaurantId: number | null
  restaurantUserId: number | null
}

const statusRow = ref<StatusRow | null>(null)

function formatTime(v: string) {
  return v.replace('T', ' ')
}

async function submit() {
  message.value = ''
  statusRow.value = null
  submitting.value = true
  try {
    const res = await fetch('http://localhost:8081/api/public/restaurant-applications', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      message.value = (data as any).message ?? '提交失败'
      return
    }
    message.value = '提交成功，请等待管理员审核'
    await checkStatus()
  } catch {
    message.value = '无法连接到服务器'
  } finally {
    submitting.value = false
  }
}

async function checkStatus() {
  message.value = ''
  statusRow.value = null
  if (!form.username) return
  try {
    const qs = new URLSearchParams({ username: form.username })
    const res = await fetch(`http://localhost:8081/api/public/restaurant-applications/latest?${qs.toString()}`)
    if (res.status === 404) {
      message.value = '未找到该用户名的申请'
      return
    }
    if (!res.ok) {
      message.value = '查询失败'
      return
    }
    statusRow.value = (await res.json()) as StatusRow
  } catch {
    message.value = '无法连接到服务器'
  }
}
</script>

<style scoped>
.auth {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(1200px 600px at 10% 10%, rgba(24, 144, 255, 0.35), transparent 60%),
    radial-gradient(900px 520px at 90% 30%, rgba(115, 209, 61, 0.35), transparent 55%),
    linear-gradient(180deg, #f7fbff 0%, #f5f5f5 100%);
}

.auth-card {
  width: min(980px, 100%);
  padding: 22px;
  border-radius: 14px;
  background-color: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 16px 50px rgba(0, 0, 0, 0.12);
}

.head h2 {
  margin: 0;
  font-size: 20px;
}

.sub {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
}

.form {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 860px) {
  .form {
    grid-template-columns: 1fr;
  }
}

.group {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px 12px;
  background: #fafafa;
  display: grid;
  gap: 10px;
}

.group-title {
  font-weight: 700;
  font-size: 13px;
  color: #333;
}

.field {
  display: grid;
  gap: 6px;
}

.field > span {
  font-size: 12px;
  color: #666;
}

input {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  outline: none;
  font-size: 13px;
  background: #fff;
}

input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}

.btns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 520px) {
  .btns {
    grid-template-columns: 1fr;
  }
}

.primary {
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  background-color: #1677ff;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.ghost {
  padding: 10px 0;
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #333;
  cursor: pointer;
  text-align: center;
  font-size: 13px;
  text-decoration: none;
}

.ghost:disabled,
.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost.link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.status {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  display: grid;
  gap: 6px;
}

.msg {
  margin-top: 12px;
  color: #ff4d4f;
  font-size: 13px;
  text-align: center;
}
</style>
