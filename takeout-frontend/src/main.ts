import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('./views/LoginView.vue'),
  },
  {
    path: '/restaurant/apply',
    name: 'RestaurantApply',
    component: () => import('./views/RestaurantApplyView.vue'),
  },
  {
    path: '/customer/register',
    name: 'CustomerRegister',
    component: () => import('./views/customer/CustomerRegisterView.vue'),
  },
  {
    path: '/c',
    name: 'CustomerHome',
    component: () => import('./views/customer/CustomerHomeView.vue'),
  },
  {
    path: '/c/orders',
    name: 'CustomerOrders',
    component: () => import('./views/customer/CustomerOrdersView.vue'),
  },
  {
    path: '/c/stats',
    name: 'CustomerStats',
    component: () => import('./views/customer/CustomerStatsView.vue'),
  },
  {
    path: '/c/addresses',
    name: 'CustomerAddresses',
    component: () => import('./views/customer/CustomerAddressesView.vue'),
  },
  {
    path: '/c/profile',
    name: 'CustomerProfile',
    component: () => import('./views/customer/CustomerProfileView.vue'),
  },
  {
    path: '/d',
    name: 'RiderHome',
    component: () => import('./views/rider/RiderOrdersView.vue'),
  },
  {
    path: '/d/hall',
    name: 'RiderHall',
    component: () => import('./views/rider/RiderHallView.vue'),
  },
  {
    path: '/',
    component: () => import('./layouts/BasicLayout.vue'),
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('./views/DashboardView.vue'),
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('./views/OrdersView.vue'),
      },
      {
        path: 'restaurants',
        name: 'Restaurants',
        component: () => import('./views/RestaurantManageView.vue'),
      },
      {
        path: 'customers',
        name: 'Customers',
        component: () => import('./views/CustomerListView.vue'),
      },
      {
        path: 'restaurant-users',
        name: 'RestaurantUsers',
        redirect: '/restaurants',
      },
      {
        path: 'commission',
        name: 'Commission',
        redirect: '/',
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('./views/PlatformStatsView.vue'),
      },
      {
        path: 'payment-logs',
        name: 'PaymentLogs',
        component: () => import('./views/PaymentLogsView.vue'),
      },
      {
        path: 'ratings',
        name: 'RatingsAdmin',
        component: () => import('./views/RatingAdminView.vue'),
      },
      {
        path: 'delivery-staff',
        name: 'DeliveryStaff',
        component: () => import('./views/DeliveryStaffManageView.vue'),
      },
      {
        path: 'delivery-staff-users',
        name: 'DeliveryStaffUsers',
        redirect: '/delivery-staff',
      },
      {
        path: 'restaurant-applications',
        name: 'RestaurantApplications',
        component: () => import('./views/RestaurantApplicationsView.vue'),
      },
      {
        path: 'merchant/dishes',
        name: 'MerchantDishes',
        component: () => import('./views/merchant/MerchantDishesView.vue'),
      },
      {
        path: 'merchant/promotions',
        name: 'MerchantPromotions',
        component: () => import('./views/merchant/MerchantPromotionsView.vue'),
      },
      {
        path: 'merchant/stats',
        name: 'MerchantStats',
        component: () => import('./views/merchant/MerchantStatsView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const publicPaths = ['/login', '/restaurant/apply', '/customer/register']
  const isPublic = publicPaths.includes(to.path)
  const hasUser = !!localStorage.getItem('currentUser')
  const role = localStorage.getItem('role')

  if (!isPublic && !hasUser) {
    next('/login')
    return
  }

  // 食客登录后默认进入食客端页面
  if (hasUser && role === 'CUSTOMER' && to.path === '/') {
    next('/c')
    return
  }
  // 饭店登录后默认进入“仪表盘&本店统计”
  if (hasUser && role === 'RESTAURANT' && to.path === '/') {
    next('/merchant/stats')
    return
  }
  // 骑手登录后默认进入骑手端页面
  if (hasUser && role === 'DELIVERY' && to.path === '/') {
    next('/d')
    return
  } else if (to.path === '/login' && hasUser) {
    next('/')
  } else {
    next()
  }
})

const app = createApp(App)
app.use(router)
app.mount('#app')
