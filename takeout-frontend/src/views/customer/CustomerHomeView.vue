<template>
  <div class="page">
    <header class="topbar">
      <div class="title">外卖点餐</div>
      <div class="actions">
        <span class="tag">食客</span>
        <span v-if="selectedRestaurantName" class="tag">当前：{{ selectedRestaurantName }}</span>
        <RouterLink class="btn" to="/c/orders">我的订单</RouterLink>
        <RouterLink class="btn" to="/c/addresses">地址簿</RouterLink>
        <RouterLink class="btn" to="/c/profile">个人信息</RouterLink>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="card left">
        <div class="card-head">
          <h3 class="section">饭店列表</h3>
          <div class="muted">{{ restaurants.length }} 家</div>
        </div>
        <div class="list" role="list">
          <button
            v-for="r in restaurants"
            :key="r.id"
            class="list-item"
            :class="{ active: r.id === selectedRestaurantId }"
            @click="selectRestaurant(r.id)"
          >
            <div class="name">{{ r.name }}</div>
            <div class="meta">{{ r.address }}</div>
          </button>
        </div>
      </div>

      <div class="right">
        <div class="card">
          <div class="card-head">
            <h3 class="section">菜品</h3>
            <div class="tools">
              <input v-model="dishKeyword" class="search" type="search" placeholder="搜索菜品…" />
            </div>
          </div>

          <div v-if="selectedRestaurantId == null" class="empty">先选择一个饭店</div>
          <div v-else-if="loadingDishes" class="empty">加载中...</div>
          <div v-else-if="filteredDishes.length === 0" class="empty">暂无菜品</div>
          <div v-else class="dish-grid">
            <div v-for="d in filteredDishes" :key="d.id" class="dish-card">
              <div class="dish-top">
                <div class="dish-name" :title="d.name">{{ d.name }}</div>
                <div class="dish-price">
                  <span v-if="(d.discountType ?? '') && d.discountValue != null" class="sub">
                    <span class="old">￥{{ d.price ?? 0 }}</span>
                    ￥{{ calcPayPrice(d).toFixed(2) }}
                  </span>
                  <span v-else>￥{{ d.price ?? 0 }}</span>
                </div>
              </div>

              <div class="dish-actions">
                <template v-if="qtyOf(d.id) > 0">
                  <button class="mini" @click="dec(d.id)">-</button>
                  <span class="qty">{{ qtyOf(d.id) }}</span>
                  <button class="mini primary" @click="inc(d.id)">+</button>
                </template>
                <button v-else class="btn" @click="addToCart(d)">加入购物车</button>
              </div>
            </div>
          </div>
        </div>

        <div class="card cart cart-desktop">
          <div class="card-head">
            <h3 class="section">购物车</h3>
            <button v-if="cart.length > 0" class="btn" @click="clearCart">清空</button>
          </div>
          <div v-if="cart.length === 0" class="empty">购物车为空</div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>菜品</th>
                <th>单价</th>
                <th>数量</th>
                <th>小计</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in cart" :key="c.dishId">
                <td>{{ c.name }}</td>
                <td>
                  <span v-if="c.payPrice !== c.price" class="sub">
                    <span class="old">￥{{ c.price }}</span>
                    ￥{{ c.payPrice.toFixed(2) }}
                  </span>
                  <span v-else>￥{{ c.price }}</span>
                </td>
                <td>
                  <button class="mini" @click="dec(c.dishId)">-</button>
                  <span class="qty">{{ c.quantity }}</span>
                  <button class="mini" @click="inc(c.dishId)">+</button>
                </td>
                <td>￥{{ (c.payPrice * c.quantity).toFixed(2) }}</td>
                <td><button class="mini danger" @click="remove(c.dishId)">移除</button></td>
              </tr>
            </tbody>
          </table>

          <div class="checkout">
            <div class="total">
              <div>原价：￥{{ total.toFixed(2) }}</div>
              <div v-if="dishDiscount > 0" class="sub">菜品优惠：-￥{{ dishDiscount.toFixed(2) }}</div>
              <div v-if="promotionDiscount > 0" class="sub">满减优惠：-￥{{ promotionDiscount.toFixed(2) }}</div>
              <div class="pay">应付：￥{{ payTotal.toFixed(2) }}</div>
            </div>
            <div class="addr">
              <div class="addr-address">
                <input
                  v-model="addressDetail"
                  placeholder="收货地址（必填，如：宿舍楼/小区/门牌号）"
                  title="可手动输入，也可用按钮地图选点/使用当前位置"
                />
                <BaiduLocationPicker v-model="pickedLocation" compact />
              </div>
              <input v-model="contactName" placeholder="收货人姓名（必填）" title="填写收货人姓名" />
              <input v-model="contactPhone" placeholder="收货人电话（必填）" title="填写收货人手机号/电话" />
            </div>
            <div class="pay-method">
              <div class="k">支付方式</div>
              <select v-model="payMethod">
                <option value="MOCK">模拟支付</option>
                <option value="WECHAT_MOCK_QR">微信支付（模拟扫码）</option>
                <option value="WECHAT_NATIVE">微信支付（扫码，需商户号）</option>
              </select>
            </div>
            <div class="remark">
              <label for="remark">备注（如口味偏好、忌口、配送提示）</label>
              <textarea id="remark" v-model="remark" placeholder="不超过255字" maxlength="255"></textarea>
            </div>
            <button class="btn primary" :disabled="cart.length === 0 || !canSubmit" @click="submitOrder">
              提交订单并支付
            </button>
            <div v-if="msg" class="msg">{{ msg }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="mobile-cartbar" role="button" @click="openCartDrawer">
      <div class="cartbar-left">
        <div class="cartbar-title">购物车</div>
        <div class="cartbar-sub">
          <span v-if="cartCount > 0">{{ cartCount }} 件</span>
          <span v-else>未选购</span>
          <span class="dot">·</span>
          <span>应付 ￥{{ payTotal.toFixed(2) }}</span>
        </div>
      </div>
      <button class="btn primary cartbar-btn" type="button" :disabled="cartCount === 0">去结算</button>
    </div>

    <div v-if="cartDrawerOpen" class="drawer-mask" @click.self="closeCartDrawer">
      <div class="drawer" role="dialog" aria-modal="true">
        <div class="drawer-head">
          <div class="drawer-title">下单</div>
          <button class="btn" type="button" @click="closeCartDrawer">关闭</button>
        </div>

        <div class="drawer-body">
          <div v-if="cart.length === 0" class="empty">购物车为空</div>
          <template v-else>
            <div class="drawer-section">
              <div class="drawer-section-title">商品</div>
              <div class="cart-list">
                <div v-for="c in cart" :key="c.dishId" class="cart-row">
                  <div class="cart-main">
                    <div class="cart-name">{{ c.name }}</div>
                    <div class="cart-price">
                      <span v-if="c.payPrice !== c.price" class="sub">
                        <span class="old">￥{{ c.price }}</span>
                        ￥{{ c.payPrice.toFixed(2) }}
                      </span>
                      <span v-else>￥{{ c.price }}</span>
                    </div>
                  </div>
                  <div class="cart-ops">
                    <button class="mini" @click="dec(c.dishId)">-</button>
                    <span class="qty">{{ c.quantity }}</span>
                    <button class="mini primary" @click="inc(c.dishId)">+</button>
                    <button class="mini danger" @click="remove(c.dishId)">删</button>
                  </div>
                </div>
              </div>
              <button class="btn" type="button" @click="clearCart">清空购物车</button>
            </div>

            <div class="drawer-section">
              <div class="drawer-section-title">金额</div>
              <div class="total">
                <div>原价：￥{{ total.toFixed(2) }}</div>
                <div v-if="dishDiscount > 0" class="sub">菜品优惠：-￥{{ dishDiscount.toFixed(2) }}</div>
                <div v-if="promotionDiscount > 0" class="sub">满减优惠：-￥{{ promotionDiscount.toFixed(2) }}</div>
                <div class="pay">应付：￥{{ payTotal.toFixed(2) }}</div>
              </div>
            </div>

            <div class="drawer-section">
              <div class="drawer-section-title">收货信息</div>
              <div class="addr addr-mobile">
                <div class="addr-address">
                  <input v-model="addressDetail" placeholder="收货地址（必填，如：宿舍楼/小区/门牌号）" />
                  <BaiduLocationPicker v-model="pickedLocation" compact />
                </div>
                <input v-model="contactName" placeholder="收货人姓名（必填）" />
                <input v-model="contactPhone" placeholder="收货人电话（必填）" />
              </div>
            </div>

            <div class="drawer-section">
              <div class="drawer-section-title">支付与备注</div>
              <div class="pay-method">
                <div class="k">支付方式</div>
                <select v-model="payMethod">
                  <option value="MOCK">模拟支付</option>
                  <option value="WECHAT_MOCK_QR">微信支付（模拟扫码）</option>
                  <option value="WECHAT_NATIVE">微信支付（扫码，需商户号）</option>
                </select>
              </div>
              <div class="remark">
                <label for="remarkDrawer">备注（如口味偏好、忌口、配送提示）</label>
                <textarea id="remarkDrawer" v-model="remark" placeholder="不超过255字" maxlength="255"></textarea>
              </div>
              <div v-if="msg" class="msg">{{ msg }}</div>
            </div>
          </template>
        </div>

        <div class="drawer-foot">
          <div class="drawer-pay">
            <div class="drawer-pay-k">应付</div>
            <div class="drawer-pay-v">￥{{ payTotal.toFixed(2) }}</div>
          </div>
          <button class="btn primary drawer-submit" :disabled="cart.length === 0 || !canSubmit" @click="submitOrder">
            提交订单并支付
          </button>
        </div>
      </div>
    </div>

    <div v-if="wxPayOpen" class="modal" @click.self="closeWxPay">
      <div class="dialog">
        <div class="dialog-title">扫码支付</div>
        <div class="dialog-sub">订单号：{{ wxPayOrderId }}</div>
        <img v-if="wxPayQrDataUrl" class="qr" :src="wxPayQrDataUrl" alt="wechat pay qr" />
        <div class="dialog-actions">
          <button v-if="wxPayCanConfirm" class="btn primary" @click="confirmPaid">我已支付（模拟）</button>
          <button class="btn" @click="closeWxPay">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaiduLocationPicker from '../../components/BaiduLocationPicker.vue'
import { api } from '../../lib/api'

interface Restaurant {
  id: number
  name: string
  address: string
  status: string
}

interface Dish {
  id: number
  name: string
  price: number
  status: string
  discountType?: string | null
  discountValue?: number | null
}

interface Promotion {
  id: number
  type: string
  thresholdAmount: number
  discountAmount: number
}

interface CartItem {
  dishId: number
  name: string
  price: number // 原价
  payPrice: number // 折后单价
  quantity: number
}

const router = useRouter()

const restaurants = ref<Restaurant[]>([])
const selectedRestaurantId = ref<number | null>(null)
const dishes = ref<Dish[]>([])
const loadingDishes = ref(false)
const promotions = ref<Promotion[]>([])

const cart = ref<CartItem[]>([])
const addressDetail = ref('')
const contactName = ref('')
const contactPhone = ref('')
const pickedLocation = ref<{ address: string; lat: number; lng: number } | null>(null)
const msg = ref('')

const total = computed(() => cart.value.reduce((sum, i) => sum + i.price * i.quantity, 0))
const dishDiscount = computed(() =>
  cart.value.reduce((sum, i) => sum + (i.price - i.payPrice) * i.quantity, 0),
)
const payBeforePromotion = computed(() =>
  cart.value.reduce((sum, i) => sum + i.payPrice * i.quantity, 0),
)

const promotionDiscount = computed(() => {
  const amount = payBeforePromotion.value
  let best = 0
  for (const p of promotions.value) {
    if (p.type !== 'FULL_REDUCTION') continue
    if (amount >= Number(p.thresholdAmount ?? 0)) {
      best = Math.max(best, Number(p.discountAmount ?? 0))
    }
  }
  return Math.min(best, amount)
})

const payTotal = computed(() => Math.max(0, payBeforePromotion.value - promotionDiscount.value))

const payMethod = ref<'MOCK' | 'WECHAT_MOCK_QR' | 'WECHAT_NATIVE'>('MOCK')
const remark = ref('')
const dishKeyword = ref('')
const wxPayOpen = ref(false)
const wxPayQrDataUrl = ref('')
const wxPayOrderId = ref<number | null>(null)
const wxPayCanConfirm = ref(false)
let wxPayPollTimer: number | null = null
const cartDrawerOpen = ref(false)
const settingAddressFromPicker = ref(false)

function stopWxPayPoll() {
  if (wxPayPollTimer != null) {
    window.clearInterval(wxPayPollTimer)
    wxPayPollTimer = null
  }
}

function openCartDrawer() {
  if (cartCount.value === 0) return
  cartDrawerOpen.value = true
}

function closeCartDrawer() {
  cartDrawerOpen.value = false
}

function closeWxPay() {
  wxPayOpen.value = false
  wxPayQrDataUrl.value = ''
  wxPayOrderId.value = null
  wxPayCanConfirm.value = false
  stopWxPayPoll()
}

async function confirmPaid() {
  const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
  if (!wxPayOrderId.value || !user.userId) return
  const paidOrderId = wxPayOrderId.value
  await fetch(api(`/api/customer/orders/${paidOrderId}/pay?method=MOCK`), { method: 'POST' }).catch(() => {})
  closeWxPay()
  msg.value = `已确认支付：订单号 ${paidOrderId}`
  cart.value = []
}

const selectedRestaurantName = computed(() => {
  const id = selectedRestaurantId.value
  if (id == null) return ''
  return restaurants.value.find((r) => r.id === id)?.name ?? ''
})

const filteredDishes = computed(() => {
  const kw = dishKeyword.value.trim().toLowerCase()
  if (!kw) return dishes.value
  return dishes.value.filter((d) => String(d.name ?? '').toLowerCase().includes(kw))
})

const cartCount = computed(() => cart.value.reduce((sum, i) => sum + (Number(i.quantity ?? 0) || 0), 0))

function qtyOf(dishId: number) {
  return cart.value.find((c) => c.dishId === dishId)?.quantity ?? 0
}

function clearCart() {
  cart.value = []
  cartDrawerOpen.value = false
}

watch(pickedLocation, (v) => {
  if (!v?.address) return
  settingAddressFromPicker.value = true
  addressDetail.value = v.address
  window.setTimeout(() => {
    settingAddressFromPicker.value = false
  }, 0)
})

watch(addressDetail, () => {
  if (settingAddressFromPicker.value) return
  if (pickedLocation.value != null) pickedLocation.value = null
})

const canSubmit = computed(() => !!addressDetail.value && !!contactName.value && !!contactPhone.value)

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

async function loadRestaurants() {
  const res = await fetch(api('/api/public/restaurants'))
  if (!res.ok) return
  restaurants.value = await res.json()
}

async function selectRestaurant(id: number) {
  selectedRestaurantId.value = id
  cart.value = []
  cartDrawerOpen.value = false
  await loadDishes(id)
  await loadPromotions(id)
}

async function loadDishes(restaurantId: number) {
  loadingDishes.value = true
  try {
    const res = await fetch(api(`/api/public/restaurants/${restaurantId}/dishes`))
    if (!res.ok) return
    dishes.value = await res.json()
    dishKeyword.value = ''
  } finally {
    loadingDishes.value = false
  }
}

async function loadPromotions(restaurantId: number) {
  promotions.value = []
  const res = await fetch(api(`/api/public/restaurants/${restaurantId}/promotions`))
  if (!res.ok) return
  promotions.value = await res.json()
}

function calcPayPrice(d: Dish) {
  const price = Number(d.price ?? 0)
  const t = (d.discountType ?? '').toUpperCase()
  const v = d.discountValue == null ? null : Number(d.discountValue)
  if (!t || v == null) return price
  if (t === 'PERCENT') {
    const ratio = v <= 1 ? v : v / 100
    return Math.max(0, price * ratio)
  }
  if (t === 'AMOUNT') {
    return Math.max(0, price - v)
  }
  return price
}

function addToCart(d: Dish) {
  const existing = cart.value.find((c) => c.dishId === d.id)
  if (existing) {
    existing.quantity += 1
  } else {
    const price = Number(d.price ?? 0)
    const payPrice = Number(calcPayPrice(d))
    cart.value.push({ dishId: d.id, name: d.name, price, payPrice, quantity: 1 })
  }
}

function inc(dishId: number) {
  const c = cart.value.find((x) => x.dishId === dishId)
  if (c) c.quantity += 1
}

function dec(dishId: number) {
  const c = cart.value.find((x) => x.dishId === dishId)
  if (!c) return
  c.quantity -= 1
  if (c.quantity <= 0) remove(dishId)
}

function remove(dishId: number) {
  cart.value = cart.value.filter((x) => x.dishId !== dishId)
}

async function submitOrder() {
  msg.value = ''
  const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
  if (!user.userId || !selectedRestaurantId.value) {
    msg.value = '请先登录并选择饭店'
    return
  }

  cartDrawerOpen.value = false
  const createRes = await fetch(api('/api/customer/orders'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      customerId: user.userId,
      restaurantId: selectedRestaurantId.value,
      addressDetail: pickedLocation.value?.address || addressDetail.value,
      contactName: contactName.value,
      contactPhone: contactPhone.value,
      deliveryLat: pickedLocation.value?.lat ?? null,
      deliveryLng: pickedLocation.value?.lng ?? null,
      remark: remark.value,
      items: cart.value.map((c) => ({ dishId: c.dishId, quantity: c.quantity })),
    }),
  })
  if (!createRes.ok) {
    const err = await createRes.json().catch(() => ({}))
    msg.value = err.message ?? '下单失败'
    return
  }
  const created = await createRes.json()

  if (payMethod.value === 'MOCK') {
    wxPayOrderId.value = Number(created.orderId)
    wxPayQrDataUrl.value = '/receipt-qr.jpg'
    wxPayCanConfirm.value = true
    wxPayOpen.value = true
    msg.value = `订单已创建：订单号 ${created.orderId}，请扫码并点击“我已支付（模拟）”完成支付`
    remark.value = ''
    return
  }

  const payRes = await fetch(
    api(`/api/customer/orders/${created.orderId}/pay?method=${encodeURIComponent(payMethod.value)}`),
    { method: 'POST' },
  )
  if (!payRes.ok) {
    const err = await payRes.json().catch(() => ({}))
    msg.value = err.message ?? '支付失败'
    return
  }

  const payData = await payRes.json().catch(() => ({}))
  if (payMethod.value === 'WECHAT_NATIVE' || payMethod.value === 'WECHAT_MOCK_QR') {
    wxPayOrderId.value = Number(created.orderId)
    wxPayQrDataUrl.value = String(payData.qrCodeDataUrl ?? '')
    wxPayOpen.value = true
    wxPayCanConfirm.value = false

    stopWxPayPoll()
    wxPayPollTimer = window.setInterval(async () => {
    const res = await fetch(api(`/api/customer/orders/${created.orderId}?customerId=${user.userId}`))
      if (!res.ok) return
      const detail = await res.json().catch(() => ({}))
      if (detail.payStatus === 'PAID') {
        msg.value = `支付成功：订单号 ${created.orderId}，实付 ￥${Number(detail.payAmount ?? created.payAmount ?? 0).toFixed(
          2,
        )}`
        cart.value = []
        remark.value = ''
        closeWxPay()
      }
    }, 2000)

    msg.value = `订单已创建：订单号 ${created.orderId}，请使用微信扫码完成支付`
    return
  }

  msg.value = `下单成功：订单号 ${created.orderId}，实付 ￥${Number(created.payAmount ?? 0).toFixed(2)}`
  cart.value = []
}

onMounted(() => {
  void loadRestaurants()
})

onUnmounted(() => {
  stopWxPayPoll()
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
  justify-content: flex-end;
}

.pay-method {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 0 0;
}

.pay-method .k {
  font-size: 13px;
  color: var(--muted);
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  z-index: 1000;
}

.dialog {
  width: min(420px, 96vw);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.28);
}

.dialog-title {
  font-weight: 900;
  margin-bottom: 4px;
}

.dialog-sub {
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 12px;
}

.qr {
  width: 260px;
  height: 260px;
  display: block;
  margin: 0 auto 12px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.content {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  padding: 18px 16px 28px;
  max-width: 1200px;
  margin: 0 auto;
}

.right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  margin: 0;
  font-size: 15px;
}

.muted {
  color: var(--muted);
  font-size: 13px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  text-align: left;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 14px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  transition:
    border-color 120ms ease,
    box-shadow 120ms ease,
    transform 120ms ease;
}

.list-item.active {
  border-color: rgba(249, 115, 22, 0.35);
  box-shadow: 0 14px 38px rgba(249, 115, 22, 0.14);
  transform: translateY(-1px);
}

.name {
  font-weight: 800;
}

.meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.dish-card {
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.86);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
}

.dish-name {
  font-weight: 800;
  line-height: 1.25;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dish-price {
  margin-top: 8px;
  color: var(--primary-600);
  font-weight: 800;
}

.dish-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  align-items: center;
}

.cart {
  overflow: hidden;
}

.table {
  font-size: 13px;
}

.qty {
  display: inline-block;
  width: 26px;
  text-align: center;
}

.checkout {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.total {
  display: grid;
  gap: 4px;
  font-weight: 700;
}

.pay {
  margin-top: 4px;
}

.old {
  text-decoration: line-through;
  color: rgba(15, 23, 42, 0.42);
  margin-right: 6px;
}

.sub {
  color: var(--muted);
  font-size: 13px;
  font-weight: 400;
}

.addr {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.addr-address {
  grid-column: 1 / -1;
  display: grid;
  gap: 10px;
}

.addr-address > input {
  min-width: 0;
}

.addr-address :deep(.wrap.compact) {
  width: 100%;
}

.empty {
  color: var(--muted);
  padding: 12px 0;
}

.msg {
  color: rgb(21, 128, 61);
  font-size: 13px;
}

.tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search {
  width: min(320px, 46vw);
}

@media (max-width: 980px) {
  .content {
    grid-template-columns: 1fr;
    padding-bottom: 98px;
  }

  .list {
    flex-direction: row;
    overflow: auto;
    padding-bottom: 4px;
  }

  .list-item {
    min-width: 220px;
  }

  .addr {
    grid-template-columns: 1fr;
  }

  .search {
    width: 100%;
  }

  .cart-desktop {
    display: none;
  }

  .addr-address {
    grid-column: 1 / -1;
  }
}

.mobile-cartbar {
  display: none;
}

.drawer-mask {
  display: none;
}

@media (max-width: 980px) {
  .mobile-cartbar {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 12px;
    z-index: 60;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 12px 12px 14px;
    border-radius: 18px;
    border: 1px solid rgba(255, 255, 255, 0.28);
    background: rgba(15, 23, 42, 0.68);
    color: rgba(255, 255, 255, 0.92);
    box-shadow: 0 20px 60px rgba(15, 23, 42, 0.35);
    backdrop-filter: blur(14px);
    cursor: pointer;
  }

  .cartbar-left {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .cartbar-title {
    font-weight: 900;
    letter-spacing: 0.4px;
  }

  .cartbar-sub {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.78);
    display: flex;
    gap: 6px;
    align-items: center;
    flex-wrap: wrap;
  }

  .dot {
    opacity: 0.7;
  }

  .cartbar-btn {
    padding: 10px 14px !important;
    border-radius: 999px !important;
  }

  .drawer-mask {
    position: fixed;
    inset: 0;
    z-index: 80;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    background: rgba(15, 23, 42, 0.55);
    backdrop-filter: blur(6px);
    padding: 10px;
  }

  .drawer {
    width: min(720px, 100%);
    max-height: min(92vh, 820px);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    border-radius: 18px;
    border: 1px solid rgba(255, 255, 255, 0.28);
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 26px 80px rgba(15, 23, 42, 0.36);
  }

  .drawer-head {
    padding: 12px 12px 10px 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    border-bottom: 1px solid rgba(15, 23, 42, 0.08);
    background: rgba(255, 255, 255, 0.92);
  }

  .drawer-title {
    font-weight: 900;
    letter-spacing: 0.4px;
  }

  .drawer-body {
    padding: 12px 14px;
    overflow: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .drawer-section {
    border: 1px solid rgba(15, 23, 42, 0.08);
    border-radius: 16px;
    padding: 12px;
    background: rgba(255, 255, 255, 0.76);
  }

  .drawer-section-title {
    font-weight: 900;
    margin-bottom: 10px;
  }

  .cart-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 10px;
  }

  .cart-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 10px 10px 10px 12px;
    border-radius: 14px;
    border: 1px solid rgba(15, 23, 42, 0.08);
    background: rgba(255, 255, 255, 0.86);
  }

  .cart-main {
    min-width: 0;
    display: grid;
    gap: 4px;
  }

  .cart-name {
    font-weight: 800;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 46vw;
  }

  .cart-price {
    font-size: 12px;
    color: rgba(15, 23, 42, 0.72);
  }

  .cart-ops {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .addr-mobile {
    grid-template-columns: 1fr;
  }

  .geo-details summary {
    cursor: pointer;
    font-weight: 700;
    color: rgba(15, 23, 42, 0.78);
  }

  .geo-details[open] summary {
    margin-bottom: 10px;
  }

  .drawer-foot {
    padding: 12px 12px 12px 14px;
    border-top: 1px solid rgba(15, 23, 42, 0.08);
    background: rgba(255, 255, 255, 0.96);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .drawer-pay {
    display: grid;
    gap: 2px;
  }

  .drawer-pay-k {
    font-size: 12px;
    color: var(--muted);
  }

  .drawer-pay-v {
    font-weight: 900;
    font-size: 16px;
    color: var(--primary-600);
  }

  .drawer-submit {
    padding: 12px 14px !important;
    border-radius: 999px !important;
  }
}
</style>
