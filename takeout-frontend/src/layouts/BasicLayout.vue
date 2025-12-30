<template>
  <div class="layout">
    <aside class="layout-sider">
      <div class="logo">Takeout Admin</div>
      <nav class="menu">
        <RouterLink v-if="isRestaurant" to="/merchant/stats">仪表盘&本店统计</RouterLink>
        <RouterLink v-else to="/">仪表盘</RouterLink>
        <RouterLink v-if="isAdmin || isRestaurant" to="/orders">订单管理</RouterLink>
        <RouterLink v-if="isAdmin" to="/restaurants">饭店&账号</RouterLink>
        <RouterLink v-if="isAdmin" to="/customers">食客管理</RouterLink>
        <RouterLink v-if="isAdmin" to="/delivery-staff">骑手&账号</RouterLink>
        <RouterLink v-if="isAdmin" to="/restaurant-applications">入驻审核</RouterLink>
        <RouterLink v-if="isAdmin" to="/stats">平台统计</RouterLink>
        <RouterLink v-if="isAdmin" to="/payment-logs">支付/退款流水</RouterLink>
        <RouterLink v-if="isAdmin" to="/ratings">评价管理</RouterLink>
        <RouterLink v-if="isRestaurant" to="/merchant/dishes">菜品管理</RouterLink>
        <RouterLink v-if="isRestaurant" to="/merchant/promotions">优惠活动</RouterLink>
      </nav>
    </aside>
    <div class="layout-main">
      <header class="layout-header">
        <div class="title">外卖订餐系统后台</div>
        <div class="user-info">
          <span class="role">{{ roleLabel }}</span>
          <button class="logout-btn" @click="logout">退出</button>
        </div>
      </header>
      <main class="layout-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const role = ref<string | null>(null)

onMounted(() => {
  role.value = localStorage.getItem('role')
})

const isAdmin = computed(() => role.value === 'ADMIN')
const isRestaurant = computed(() => role.value === 'RESTAURANT')

const roleLabel = computed(() => {
  if (role.value === 'ADMIN') return '管理员'
  if (role.value === 'RESTAURANT') return '饭店'
  if (role.value === 'CUSTOMER') return '食客'
  return '未登录'
})

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.layout-sider {
  width: 220px;
  background:
    radial-gradient(900px 520px at 20% 10%, rgba(249, 115, 22, 0.18), transparent 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  color: var(--text);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  backdrop-filter: blur(14px);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  letter-spacing: 0.6px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  color: var(--text);
}

.menu {
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 10px;
}

.menu a {
  color: rgba(15, 23, 42, 0.78);
  text-decoration: none;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.5);
  transition:
    background-color 120ms ease,
    border-color 120ms ease,
    box-shadow 120ms ease,
    transform 120ms ease;
}

.menu a.router-link-active {
  background: var(--primary-soft);
  border-color: rgba(249, 115, 22, 0.22);
  color: var(--primary-600);
  box-shadow: 0 12px 30px rgba(249, 115, 22, 0.14);
}

.menu a:hover {
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(15, 23, 42, 0.12);
  transform: translateY(-1px);
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(14px);
}

.layout-content {
  flex: 1;
  padding: 18px;
  background-color: transparent;
  overflow: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logout-btn {
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.8);
}
</style>
