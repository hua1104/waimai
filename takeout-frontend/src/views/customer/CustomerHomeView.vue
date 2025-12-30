<template>
  <div class="page">
    <header class="topbar">
      <div class="title">外卖点餐（食客端）</div>
      <div class="actions">
        <span class="user">食客</span>
        <RouterLink class="btn" to="/c/profile">个人信息</RouterLink>
        <RouterLink class="btn" to="/c/orders">我的订单</RouterLink>
        <RouterLink class="btn" to="/c/addresses">地址簿</RouterLink>
        <RouterLink class="btn" to="/c/stats">我的统计</RouterLink>
        <button class="btn" @click="logout">退出</button>
      </div>
    </header>

    <div class="content">
      <div class="left">
        <h3>饭店列表</h3>
        <div class="list">
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
        <h3>菜品</h3>
        <div v-if="selectedRestaurantId == null" class="empty">请选择一个饭店</div>
        <div v-else>
          <div v-if="loadingDishes" class="empty">加载中...</div>
          <div v-else-if="dishes.length === 0" class="empty">暂无菜品</div>
          <div v-else class="dish-grid">
            <div v-for="d in dishes" :key="d.id" class="dish-card">
              <div class="dish-name">{{ d.name }}</div>
              <div class="dish-price">
                <span v-if="(d.discountType ?? '') && d.discountValue != null" class="sub">
                  <span class="old">￥{{ d.price ?? 0 }}</span>
                  ￥{{ calcPayPrice(d).toFixed(2) }}
                </span>
                <span v-else>￥{{ d.price ?? 0 }}</span>
              </div>
              <button class="btn" @click="addToCart(d)">加入购物车</button>
            </div>
          </div>
        </div>

        <div class="cart">
          <h3>购物车</h3>
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
              <select v-model.number="selectedAddressId" @change="onAddressChange">
                <option :value="0">手动填写</option>
                <option v-for="a in addresses" :key="a.id" :value="a.id">
                  {{ a.isDefault ? '默认 - ' : '' }}{{ a.label ? a.label + ' - ' : '' }}{{ a.addressDetail }}
                </option>
              </select>
              <input
                v-model="addressDetail"
                placeholder="收货地址（必填，如：宿舍楼/小区/门牌号）"
                title="填写详细收货地址；也可用下方“地图选点”自动填写"
              />
              <input v-model="contactName" placeholder="收货人姓名（必填）" title="填写收货人姓名" />
              <input v-model="contactPhone" placeholder="收货人电话（必填）" title="填写收货人手机号/电话" />
            </div>
            <div class="geo">
              <div class="geo-title">地图选点（可选）</div>
              <div v-if="selectedAddressId !== 0" class="geo-hint">
                已选地址簿地址时，也可以用地图重新选址（下单时会以地图选点为准）。
              </div>
              <BaiduLocationPicker v-model="pickedLocation" />
            </div>
            <div class="pay-method">
              <div class="k">支付方式</div>
              <select v-model="payMethod">
                <option value="MOCK">模拟支付</option>
                <option value="WECHAT_MOCK_QR">微信支付（模拟扫码）</option>
                <option value="WECHAT_NATIVE">微信支付（扫码，需商户号）</option>
              </select>
            </div>
            <button class="btn primary" :disabled="cart.length === 0 || !canSubmit" @click="submitOrder">
              提交订单并支付
            </button>
            <div v-if="msg" class="msg">{{ msg }}</div>
          </div>
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
          <RouterLink class="btn primary" to="/c/orders" @click="closeWxPay">查看订单</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaiduLocationPicker from '../../components/BaiduLocationPicker.vue'

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

interface AddressRow {
  id: number
  label: string | null
  addressDetail: string
  contactName: string | null
  contactPhone: string | null
  isDefault: boolean
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
const addresses = ref<AddressRow[]>([])
const selectedAddressId = ref<number>(0)

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
const wxPayOpen = ref(false)
const wxPayQrDataUrl = ref('')
const wxPayOrderId = ref<number | null>(null)
const wxPayCanConfirm = ref(false)
let wxPayPollTimer: number | null = null

function stopWxPayPoll() {
  if (wxPayPollTimer != null) {
    window.clearInterval(wxPayPollTimer)
    wxPayPollTimer = null
  }
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
  await fetch(`http://localhost:8081/api/customer/orders/${wxPayOrderId.value}/pay?method=MOCK`, { method: 'POST' }).catch(
    () => {},
  )
  closeWxPay()
  msg.value = `已确认支付：订单号 ${wxPayOrderId.value}`
  cart.value = []
}

const canSubmit = computed(() => !!addressDetail.value && !!contactName.value && !!contactPhone.value)

function logout() {
  localStorage.removeItem('currentUser')
  localStorage.removeItem('role')
  router.push('/login')
}

async function loadAddresses() {
  try {
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
    if (!user.userId) return
    const res = await fetch(`http://localhost:8081/api/customer/addresses?customerId=${user.userId}`)
    if (!res.ok) return
    const raw = (await res.json()) as any[]
    addresses.value = raw.map((a) => ({
      ...a,
      isDefault: typeof a?.isDefault === 'boolean' ? a.isDefault : !!a?.default,
    }))
    const def = addresses.value.find((x) => x.isDefault)
    if (def) {
      selectedAddressId.value = def.id
      applyAddress(def)
    }
  } catch {
    // ignore
  }
}

function applyAddress(a: AddressRow) {
  addressDetail.value = a.addressDetail ?? ''
  contactName.value = a.contactName ?? ''
  contactPhone.value = a.contactPhone ?? ''
  pickedLocation.value = null
}

async function loadRestaurants() {
  const res = await fetch('http://localhost:8081/api/public/restaurants')
  if (!res.ok) return
  restaurants.value = await res.json()
}

async function selectRestaurant(id: number) {
  selectedRestaurantId.value = id
  cart.value = []
  await loadDishes(id)
  await loadPromotions(id)
}

async function loadDishes(restaurantId: number) {
  loadingDishes.value = true
  try {
    const res = await fetch(`http://localhost:8081/api/public/restaurants/${restaurantId}/dishes`)
    if (!res.ok) return
    dishes.value = await res.json()
  } finally {
    loadingDishes.value = false
  }
}

async function loadPromotions(restaurantId: number) {
  promotions.value = []
  const res = await fetch(`http://localhost:8081/api/public/restaurants/${restaurantId}/promotions`)
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

function onAddressChange() {
  if (!selectedAddressId.value) return
  const a = addresses.value.find((x) => x.id === selectedAddressId.value)
  if (a) applyAddress(a)
}

async function submitOrder() {
  msg.value = ''
  const user = JSON.parse(localStorage.getItem('currentUser') || '{}')
  if (!user.userId || !selectedRestaurantId.value) {
    msg.value = '请先登录并选择饭店'
    return
  }

  const createRes = await fetch('http://localhost:8081/api/customer/orders', {
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
    return
  }

  const payRes = await fetch(
    `http://localhost:8081/api/customer/orders/${created.orderId}/pay?method=${encodeURIComponent(payMethod.value)}`,
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
      const res = await fetch(`http://localhost:8081/api/customer/orders/${created.orderId}?customerId=${user.userId}`)
      if (!res.ok) return
      const detail = await res.json().catch(() => ({}))
      if (detail.payStatus === 'PAID') {
        msg.value = `支付成功：订单号 ${created.orderId}，实付 ￥${Number(detail.payAmount ?? created.payAmount ?? 0).toFixed(
          2,
        )}`
        cart.value = []
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
  void loadAddresses()
})

onUnmounted(() => {
  stopWxPayPoll()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.title {
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.btn {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}

.btn.primary {
  border-color: #1890ff;
  background: #1890ff;
  color: #fff;
}

.pay-method {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0 8px;
}

.pay-method .k {
  font-size: 13px;
  color: #666;
}

.pay-method select {
  padding: 6px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  z-index: 1000;
}

.dialog {
  width: min(420px, 96vw);
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.2);
}

.dialog-title {
  font-weight: 700;
  margin-bottom: 4px;
}

.dialog-sub {
  color: #666;
  font-size: 13px;
  margin-bottom: 12px;
}

.qr {
  width: 260px;
  height: 260px;
  display: block;
  margin: 0 auto 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.content {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
  padding: 12px;
}

.left,
.right {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  text-align: left;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 10px;
  background: #fff;
  cursor: pointer;
}

.list-item.active {
  border-color: #1890ff;
}

.name {
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 10px;
}

.dish-card {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 10px;
}

.dish-name {
  font-weight: 600;
}

.dish-price {
  margin: 6px 0;
  color: #1890ff;
}

.cart {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table th,
.table td {
  border: 1px solid #f0f0f0;
  padding: 6px 8px;
}

.mini {
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}

.mini.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
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
  color: #999;
  margin-right: 6px;
}

.sub {
  color: #666;
  font-size: 13px;
  font-weight: 400;
}

.addr {
  display: grid;
  grid-template-columns: 1.2fr 2fr 1fr 1fr;
  gap: 8px;
}

.addr select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.addr input {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.geo {
  margin-top: 8px;
  padding: 10px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
}

.geo-title {
  font-weight: 700;
  font-size: 13px;
  margin-bottom: 6px;
}

.geo-hint {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.empty {
  color: #999;
  padding: 10px 0;
}

.msg {
  color: #389e0d;
  font-size: 13px;
}
</style>
