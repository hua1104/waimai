<template>
  <div class="page">
    <header class="topbar">
      <div class="title">个人信息（食客端）</div>
      <div class="actions">
        <RouterLink class="btn" to="/c">返回点餐</RouterLink>
        <RouterLink class="btn" to="/c/orders">我的订单</RouterLink>
        <RouterLink class="btn" to="/c/addresses">地址簿</RouterLink>
        <RouterLink class="btn" to="/c/stats">我的统计</RouterLink>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card">
        <div v-if="!customerId" class="warn">请先登录</div>

        <div v-else>
          <div class="grid2">
            <section class="card panel">
              <div class="panel-head">
                <h3 class="sub">基本信息</h3>
                <div class="chips">
                  <span class="tag">状态：{{ profile.status || '-' }}</span>
                  <span class="tag">创建：{{ profile.createdAt || '-' }}</span>
                </div>
              </div>

              <div class="form">
                <label>
                  <span>用户名</span>
                  <input v-model="profile.username" disabled />
                </label>
                <label>
                  <span>真实姓名 *</span>
                  <input v-model="profile.realName" placeholder="请输入真实姓名" />
                </label>
                <label>
                  <span>手机号 *</span>
                  <input v-model="profile.phone" placeholder="请输入手机号" />
                </label>
              </div>

              <div class="panel-foot">
                <button
                  class="btn primary"
                  :disabled="savingProfile || !profile.realName || !profile.phone"
                  @click="saveProfile"
                >
                  保存
                </button>
                <button class="btn" :disabled="loading" @click="load">刷新</button>
              </div>
            </section>

            <section class="card panel">
              <div class="panel-head">
                <h3 class="sub">修改密码</h3>
                <div class="hint">至少 6 位，建议包含字母+数字</div>
              </div>

              <div class="form">
                <label>
                  <span>旧密码 *</span>
                  <input v-model="pwd.oldPassword" type="password" placeholder="请输入旧密码" />
                </label>
                <label>
                  <span>新密码 *</span>
                  <input v-model="pwd.newPassword" type="password" placeholder="请输入新密码" />
                </label>
                <label>
                  <span>确认新密码 *</span>
                  <input v-model="pwd.confirmPassword" type="password" placeholder="再次输入新密码" />
                </label>
              </div>

              <div class="panel-foot">
                <button class="btn primary" :disabled="savingPwd || !canSavePwd" @click="savePassword">修改</button>
              </div>
            </section>
          </div>
        </div>

        <div v-if="msg" class="msg" :class="msgType">{{ msg }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../lib/api'

interface ProfileRow {
  id: number
  username: string
  realName: string | null
  phone: string | null
  status: string | null
  createdAt: string | null
}

const router = useRouter()
const loading = ref(false)
const savingProfile = ref(false)
const savingPwd = ref(false)
const msg = ref('')
const msgType = ref<'ok' | 'err'>('ok')

const customerId = computed<number | null>(() => {
  try {
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
    const v = user?.userId
    return typeof v === 'number' ? v : v ? Number(v) : null
  } catch {
    return null
  }
})

const profile = reactive<ProfileRow>({
  id: 0,
  username: '',
  realName: '',
  phone: '',
  status: '',
  createdAt: '',
})

const pwd = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const canSavePwd = computed(() => {
  if (!pwd.oldPassword || !pwd.newPassword || !pwd.confirmPassword) return false
  if (pwd.newPassword.length < 6) return false
  return pwd.newPassword === pwd.confirmPassword
})

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

async function load() {
  if (!customerId.value) return
  loading.value = true
  msg.value = ''
  try {
    const res = await fetch(api(`/api/customer/profile?customerId=${customerId.value}`))
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      msgType.value = 'err'
      msg.value = (data as any).message ?? '加载失败'
      return
    }
    const p = data as ProfileRow
    profile.id = p.id
    profile.username = p.username
    profile.realName = (p.realName ?? '') as any
    profile.phone = (p.phone ?? '') as any
    profile.status = (p.status ?? '') as any
    profile.createdAt = (p.createdAt ?? '') as any
  } catch {
    msgType.value = 'err'
    msg.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!customerId.value) return
  savingProfile.value = true
  msg.value = ''
  try {
    const payload = {
      customerId: customerId.value,
      realName: profile.realName,
      phone: profile.phone,
    }
    const res = await fetch(api('/api/customer/profile'), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      msgType.value = 'err'
      msg.value = (data as any).message ?? '保存失败'
      return
    }
    msgType.value = 'ok'
    msg.value = '保存成功'
    await load()
  } catch {
    msgType.value = 'err'
    msg.value = '无法连接到服务器'
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!customerId.value) return
  if (!canSavePwd.value) {
    msgType.value = 'err'
    msg.value = '请检查密码输入'
    return
  }
  savingPwd.value = true
  msg.value = ''
  try {
    const payload = {
      customerId: customerId.value,
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword,
    }
    const res = await fetch(api('/api/customer/profile/password'), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      msgType.value = 'err'
      msg.value = (data as any).message ?? '修改失败'
      return
    }
    msgType.value = 'ok'
    msg.value = '密码修改成功，请使用新密码重新登录'
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirmPassword = ''
  } catch {
    msgType.value = 'err'
    msg.value = '无法连接到服务器'
  } finally {
    savingPwd.value = false
  }
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
  max-width: 1100px;
  margin: 0 auto;
}

.card {
  padding: 14px;
}

.warn {
  color: var(--danger);
}

.grid2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 920px) {
  .grid2 {
    grid-template-columns: 1fr;
  }
}

.panel {
  padding: 12px;
}

.panel-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.sub {
  margin: 0;
  font-size: 15px;
}

.hint {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

@media (max-width: 520px) {
  .form {
    grid-template-columns: 1fr;
  }
}

label {
  display: grid;
  gap: 6px;
}

label > span {
  font-size: 12px;
  color: var(--muted);
}

input:disabled {
  background: rgba(15, 23, 42, 0.04);
  color: rgba(15, 23, 42, 0.55);
}

.panel-foot {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.msg {
  margin-top: 12px;
  font-size: 13px;
}

.msg.ok {
  color: #389e0d;
}

.msg.err {
  color: #ff4d4f;
}
</style>
