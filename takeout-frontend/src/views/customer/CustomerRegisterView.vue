<template>
  <div class="auth">
    <div class="auth-card">
      <div class="head">
        <h2>食客注册</h2>
        <div class="sub">注册成功后会自动跳转到登录页（角色已选“食客”）</div>
      </div>

      <form class="form" @submit.prevent="submit">
        <label class="field">
          <span>用户名 *</span>
          <input v-model="form.username" required autocomplete="username" placeholder="6~50位，唯一" />
        </label>
        <label class="field">
          <span>密码 *（至少6位）</span>
          <input v-model="form.password" type="password" required autocomplete="new-password" placeholder="至少 6 位" />
        </label>
        <label class="field">
          <span>真实姓名 *</span>
          <input v-model="form.realName" required placeholder="请输入真实姓名" />
        </label>
        <label class="field">
          <span>手机号 *</span>
          <input v-model="form.phone" required placeholder="请输入手机号" />
        </label>

        <button class="primary" type="submit" :disabled="submitting">
          {{ submitting ? '提交中...' : '注册' }}
        </button>
      </form>

      <div class="tools">
        <RouterLink class="link" to="/login">返回登录</RouterLink>
      </div>

      <div v-if="message" class="msg" :class="msgType">{{ message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
})

const submitting = ref(false)
const message = ref('')
const msgType = ref<'ok' | 'err'>('err')

async function submit() {
  message.value = ''
  submitting.value = true
  try {
    const res = await fetch('http://localhost:8081/api/public/customers/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      msgType.value = 'err'
      message.value = (data as any).message ?? '注册失败'
      return
    }
    msgType.value = 'ok'
    message.value = '注册成功，正在跳转...'
    router.push({ path: '/login', query: { role: 'CUSTOMER', username: form.username } })
  } catch {
    msgType.value = 'err'
    message.value = '无法连接到服务器'
  } finally {
    submitting.value = false
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
  width: min(520px, 100%);
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 16px 50px rgba(0, 0, 0, 0.12);
  padding: 22px;
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

@media (max-width: 540px) {
  .form {
    grid-template-columns: 1fr;
  }
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
}

input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}

.primary {
  grid-column: 1 / -1;
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  background-color: #1677ff;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.tools {
  margin-top: 12px;
  text-align: center;
}

.link {
  font-size: 13px;
  color: #1677ff;
  text-decoration: none;
}

.msg {
  margin-top: 12px;
  font-size: 13px;
  text-align: center;
}

.msg.ok {
  color: #389e0d;
}

.msg.err {
  color: #ff4d4f;
}
</style>
