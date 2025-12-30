<template>
  <div class="auth">
    <div class="bg-title">
      <div class="bg-title__main">外卖订餐系统</div>
      <div class="bg-title__sub">校园 · 饭店 · 骑手 · 管理</div>
    </div>
    <div class="panel">
      <div class="panel-head">
        <h2>登录</h2>
        <div class="sub">请输入账号密码登录</div>
      </div>

      <form class="form" @submit.prevent="handleSubmit">
        <div class="form-item">
          <label>用户名</label>
          <input v-model="form.username" type="text" autocomplete="username" required placeholder="请输入用户名" />
        </div>
        <div class="form-item">
          <label>密码</label>
          <input v-model="form.password" type="password" autocomplete="current-password" required placeholder="请输入密码" />
        </div>

        <button class="primary" type="submit">登录</button>
      </form>

      <div class="links">
        <RouterLink class="link" to="/customer/register">食客注册</RouterLink>
        <RouterLink class="link" to="/restaurant/apply">饭店入驻申请</RouterLink>
      </div>

      <div v-if="error" class="msg err">{{ error }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../lib/api'

const route = useRoute()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

const error = ref('')

onMounted(() => {
  const username = route.query.username
  if (typeof username === 'string' && username) form.username = username
})

async function handleSubmit() {
  error.value = ''
  try {
    const res = await fetch(api('/api/auth/login'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(form),
    })

    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      error.value = (data as any).message ?? '登录失败，请检查账号和密码'
      return
    }

    const data = await res.json()
    localStorage.setItem('currentUser', JSON.stringify(data))
    localStorage.setItem('role', data.role)

    if (data.role === 'ADMIN') {
      router.push('/')
    } else if (data.role === 'RESTAURANT') {
      router.push('/merchant/stats')
    } else if (data.role === 'CUSTOMER') {
      router.push('/c')
    } else if (data.role === 'DELIVERY') {
      router.push('/d')
    } else {
      router.push('/')
    }
  } catch (e) {
    error.value = '无法连接服务器，请稍后重试'
  }
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(1200px 640px at 18% 22%, rgba(255, 255, 255, 0.55), transparent 62%),
    radial-gradient(900px 520px at 78% 58%, rgba(249, 115, 22, 0.22), transparent 62%),
    linear-gradient(180deg, rgba(2, 6, 23, 0.38) 0%, rgba(2, 6, 23, 0.52) 100%),
    url('/login-bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.bg-title {
  position: absolute;
  left: 56px;
  top: 44px;
  z-index: 2;
  pointer-events: none;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 10px 26px rgba(2, 6, 23, 0.42);
}

.bg-title__main {
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 2px;
  line-height: 1.12;
}

.bg-title__sub {
  margin-top: 10px;
  font-size: 14px;
  letter-spacing: 1.5px;
  color: rgba(255, 255, 255, 0.82);
}

.auth::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1100px 560px at 22% 18%, rgba(255, 255, 255, 0.35), transparent 60%),
    radial-gradient(860px 520px at 78% 60%, rgba(249, 115, 22, 0.18), transparent 62%),
    radial-gradient(900px 520px at 55% 110%, rgba(14, 165, 233, 0.14), transparent 62%),
    repeating-linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.08) 0 1px,
      rgba(255, 255, 255, 0) 1px 12px
    );
  opacity: 0.55;
  mix-blend-mode: overlay;
  pointer-events: none;
}

.auth::after {
  content: '';
  position: absolute;
  inset: -20%;
  background:
    radial-gradient(420px 420px at 22% 35%, rgba(249, 115, 22, 0.28), transparent 62%),
    radial-gradient(520px 520px at 78% 28%, rgba(59, 130, 246, 0.22), transparent 62%),
    radial-gradient(620px 620px at 72% 82%, rgba(255, 255, 255, 0.18), transparent 62%);
  filter: blur(18px);
  opacity: 0.9;
  animation: float-bg 12s ease-in-out infinite;
  pointer-events: none;
}

/* Floating "bubbles" to enrich background without new assets */
.auth :deep(.panel)::before,
.auth :deep(.panel)::after {
  content: '';
  position: absolute;
  inset: auto;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(22px);
  opacity: 0.7;
  z-index: -1;
}

.auth :deep(.panel)::before {
  left: -260px;
  top: -220px;
  background:
    radial-gradient(circle at 30% 30%, rgba(249, 115, 22, 0.55), transparent 58%),
    radial-gradient(circle at 70% 70%, rgba(59, 130, 246, 0.35), transparent 58%);
  animation: float-bubble-1 10s ease-in-out infinite;
}

.auth :deep(.panel)::after {
  right: -280px;
  bottom: -260px;
  width: 620px;
  height: 620px;
  background:
    radial-gradient(circle at 40% 40%, rgba(255, 255, 255, 0.45), transparent 60%),
    radial-gradient(circle at 70% 30%, rgba(249, 115, 22, 0.35), transparent 58%),
    radial-gradient(circle at 35% 75%, rgba(14, 165, 233, 0.28), transparent 58%);
  animation: float-bubble-2 12s ease-in-out infinite;
}

@keyframes float-bg {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(14px, -12px, 0) scale(1.03);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes float-bubble-1 {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(18px, 12px, 0) scale(1.04);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes float-bubble-2 {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-16px, -10px, 0) scale(1.03);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
}

.panel {
  width: min(560px, 100%);
  position: relative;
  z-index: 1;
  padding: 34px 30px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.24);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
}

.panel-head h2 {
  margin: 10px 0 0;
  font-size: 24px;
}

.sub {
  margin-top: 6px;
  color: rgba(15, 23, 42, 0.66);
  font-size: 14px;
}

.form {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

label {
  font-size: 12px;
  color: #666;
}

input,
select {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #d9d9d9;
  outline: none;
  font-size: 14px;
  background: #fff;
}

input:focus,
select:focus {
  border-color: rgba(249, 115, 22, 0.46);
  box-shadow: 0 0 0 4px rgba(249, 115, 22, 0.18);
}

.primary {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, var(--primary) 0%, var(--primary-600) 100%);
  color: #fff;
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.primary:active {
  transform: translateY(1px);
}

.links {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.link {
  font-size: 13px;
  color: #1677ff;
  text-decoration: none;
}

.msg {
  margin-top: 12px;
  font-size: 13px;
}

.msg.err {
  color: #ff4d4f;
}

@media (max-width: 520px) {
  .bg-title {
    left: 18px;
    right: 18px;
    top: 18px;
    text-align: center;
  }

  .bg-title__main {
    font-size: 22px;
    letter-spacing: 1px;
  }

  .bg-title__sub {
    margin-top: 6px;
    font-size: 12px;
    letter-spacing: 1px;
  }

  .panel {
    width: min(440px, 100%);
    padding: 26px 22px;
    border-radius: 20px;
  }

  .panel-head h2 {
    font-size: 20px;
  }
}
</style>
