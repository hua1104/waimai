<template>
  <div class="auth">
    <div class="panel">
      <div class="panel-head">
        <div class="logo">Takeout</div>
        <h2>登录</h2>
        <div class="sub">请选择角色后输入账号密码</div>
      </div>

      <form class="form" @submit.prevent="handleSubmit">
        <div class="form-item">
          <label>角色</label>
          <div class="role-tabs">
            <button class="role-tab" :class="{ active: form.role === 'ADMIN' }" type="button" @click="form.role = 'ADMIN'">
              管理员
            </button>
            <button
              class="role-tab"
              :class="{ active: form.role === 'RESTAURANT' }"
              type="button"
              @click="form.role = 'RESTAURANT'"
            >
              饭店
            </button>
            <button
              class="role-tab"
              :class="{ active: form.role === 'CUSTOMER' }"
              type="button"
              @click="form.role = 'CUSTOMER'"
            >
              食客
            </button>
            <button
              class="role-tab"
              :class="{ active: form.role === 'DELIVERY' }"
              type="button"
              @click="form.role = 'DELIVERY'"
            >
              骑手
            </button>
          </div>
        </div>
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

const route = useRoute()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  role: 'ADMIN',
})

const error = ref('')

onMounted(() => {
  const role = route.query.role
  const username = route.query.username
  if (typeof role === 'string' && role) form.role = role
  if (typeof username === 'string' && username) form.username = username
})

async function handleSubmit() {
  error.value = ''
  try {
    const res = await fetch('http://localhost:8081/api/auth/login', {
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
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(1200px 600px at 10% 10%, rgba(24, 144, 255, 0.22), transparent 60%),
    radial-gradient(900px 520px at 90% 30%, rgba(115, 209, 61, 0.18), transparent 55%),
    linear-gradient(180deg, #f7fbff 0%, #f5f5f5 100%);
}

.panel {
  width: min(440px, 100%);
  padding: 26px 22px;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 16px 50px rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.panel-head h2 {
  margin: 10px 0 0;
  font-size: 20px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.1);
  color: #1677ff;
  font-weight: 700;
  letter-spacing: 0.5px;
  font-size: 13px;
}

.sub {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
}

.form {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  outline: none;
  font-size: 13px;
  background: #fff;
}

input:focus,
select:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.role-tab {
  padding: 10px 0;
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  color: #333;
  transition:
    background-color 120ms ease,
    border-color 120ms ease,
    box-shadow 120ms ease,
    transform 120ms ease;
}

.role-tab:hover {
  border-color: rgba(22, 119, 255, 0.45);
}

.role-tab.active {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
  background: rgba(22, 119, 255, 0.06);
  color: #1677ff;
}

.role-tab:active {
  transform: translateY(1px);
}

@media (max-width: 520px) {
  .role-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.primary {
  width: 100%;
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
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
</style>
