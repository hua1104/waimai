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
  background-color: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.menu {
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 10px;
}

.menu a {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  padding: 10px 14px;
  border-radius: 4px;
  font-size: 14px;
}

.menu a.router-link-active {
  background-color: #1890ff;
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
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  background-color: #fff;
}

.layout-content {
  flex: 1;
  padding: 16px;
  background-color: #f5f5f5;
  overflow: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logout-btn {
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  cursor: pointer;
  font-size: 12px;
}
</style>
