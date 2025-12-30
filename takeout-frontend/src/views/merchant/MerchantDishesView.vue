<template>
  <div class="page">
    <div class="card">
      <h2>菜品管理（饭店端）</h2>

      <div v-if="!restaurantId" class="warn">当前账号缺少 restaurantId，请重新用“饭店”角色登录。</div>

      <div v-else class="grid">
        <section class="panel">
          <div class="panel-head">
            <h3>分类</h3>
            <button class="btn" @click="loadAll">刷新</button>
          </div>

          <div class="row">
            <input v-model="newCategory.name" placeholder="分类名称" />
            <input v-model.number="newCategory.sortOrder" type="number" placeholder="排序" />
            <button class="btn primary" :disabled="!newCategory.name" @click="createCategory">新增分类</button>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>排序</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in categories" :key="c.id">
                <td>{{ c.id }}</td>
                <td>{{ c.name }}</td>
                <td>{{ c.sortOrder ?? '' }}</td>
                <td class="actions">
                  <button class="btn danger" @click="deleteCategory(c.id)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && categories.length === 0">
                <td colspan="4" class="empty">暂无分类</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h3>菜品</h3>
            <div class="hint">顾客端只展示状态为 AVAILABLE 的菜品</div>
          </div>

          <div class="row">
            <select v-model="dishForm.categoryId">
              <option :value="null">未分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
            <input v-model="dishForm.name" placeholder="菜品名称" />
            <input v-model.number="dishForm.price" type="number" step="0.01" placeholder="价格" />
            <input v-model.number="dishForm.stock" type="number" placeholder="库存" />
            <select v-model="dishForm.status">
              <option value="AVAILABLE">AVAILABLE</option>
              <option value="UNAVAILABLE">UNAVAILABLE</option>
            </select>
            <select v-model="dishForm.discountType">
              <option value="">无</option>
              <option value="PERCENT">PERCENT</option>
              <option value="AMOUNT">AMOUNT</option>
            </select>
            <input
              v-model.number="dishForm.discountValue"
              type="number"
              step="0.01"
              placeholder="折扣值"
            />
            <button class="btn primary" :disabled="!dishForm.name" @click="saveDish">
              {{ editingDishId ? '保存修改' : '新增菜品' }}
            </button>
            <button v-if="editingDishId" class="btn" @click="resetDishForm">取消编辑</button>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>分类</th>
                <th>价格</th>
                <th>库存</th>
                <th>状态</th>
                <th>折扣</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dishes" :key="d.id">
                <td>{{ d.id }}</td>
                <td>{{ d.name }}</td>
                <td>{{ d.categoryName ?? '-' }}</td>
                <td>{{ d.price ?? 0 }}</td>
                <td>{{ d.stock ?? '' }}</td>
                <td>{{ d.status ?? '' }}</td>
                <td>
                  <span v-if="d.discountType">{{ d.discountType }} {{ d.discountValue ?? '' }}</span>
                  <span v-else>-</span>
                </td>
                <td class="actions">
                  <button class="btn" @click="startEditDish(d)">编辑</button>
                  <button class="btn danger" @click="deleteDish(d.id)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && dishes.length === 0">
                <td colspan="8" class="empty">暂无菜品</td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>

      <div v-if="msg" class="msg">{{ msg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

interface CategoryRow {
  id: number
  name: string
  sortOrder: number | null
}

interface DishRow {
  id: number
  categoryId: number | null
  categoryName: string | null
  name: string
  price: number
  stock: number | null
  status: string | null
  discountType: string | null
  discountValue: number | null
}

const loading = ref(false)
const msg = ref('')

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('currentUser') || '{}')
  } catch {
    return {}
  }
})

const restaurantId = computed<number | null>(() => {
  const v = currentUser.value?.restaurantId
  return typeof v === 'number' ? v : v ? Number(v) : null
})

const categories = ref<CategoryRow[]>([])
const dishes = ref<DishRow[]>([])

const newCategory = reactive({
  name: '',
  sortOrder: null as number | null,
})

const editingDishId = ref<number | null>(null)
const dishForm = reactive({
  categoryId: null as number | null,
  name: '',
  price: 0,
  stock: null as number | null,
  status: 'AVAILABLE',
  discountType: '' as string,
  discountValue: null as number | null,
})

function api(path: string) {
  return `http://localhost:8081${path}`
}

async function loadAll() {
  if (!restaurantId.value) return
  loading.value = true
  msg.value = ''
  try {
    const [cRes, dRes] = await Promise.all([
      fetch(api(`/api/merchant/${restaurantId.value}/categories`)),
      fetch(api(`/api/merchant/${restaurantId.value}/dishes`)),
    ])
    if (cRes.ok) categories.value = await cRes.json()
    if (dRes.ok) dishes.value = await dRes.json()
  } finally {
    loading.value = false
  }
}

async function createCategory() {
  if (!restaurantId.value) return
  msg.value = ''
  const res = await fetch(api(`/api/merchant/${restaurantId.value}/categories`), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: newCategory.name,
      sortOrder: newCategory.sortOrder,
    }),
  })
  if (!res.ok) {
    msg.value = '新增分类失败'
    return
  }
  newCategory.name = ''
  newCategory.sortOrder = null
  await loadAll()
}

async function deleteCategory(categoryId: number) {
  if (!restaurantId.value) return
  msg.value = ''
  if (!confirm('确定删除该分类？（分类下有菜品会删除失败）')) return
  const res = await fetch(api(`/api/merchant/${restaurantId.value}/categories/${categoryId}`), {
    method: 'DELETE',
  })
  if (res.status === 409) {
    msg.value = '该分类下仍有菜品，请先删除菜品或移动分类'
    return
  }
  if (!res.ok) {
    msg.value = '删除分类失败'
    return
  }
  await loadAll()
}

function startEditDish(d: DishRow) {
  editingDishId.value = d.id
  dishForm.categoryId = d.categoryId ?? null
  dishForm.name = d.name ?? ''
  dishForm.price = Number(d.price ?? 0)
  dishForm.stock = d.stock ?? null
  dishForm.status = (d.status as any) || 'AVAILABLE'
  dishForm.discountType = (d.discountType as any) || ''
  dishForm.discountValue = d.discountValue ?? null
}

function resetDishForm() {
  editingDishId.value = null
  dishForm.categoryId = null
  dishForm.name = ''
  dishForm.price = 0
  dishForm.stock = null
  dishForm.status = 'AVAILABLE'
  dishForm.discountType = ''
  dishForm.discountValue = null
}

async function saveDish() {
  if (!restaurantId.value || !dishForm.name) return
  msg.value = ''

  const payload = {
    categoryId: dishForm.categoryId,
    name: dishForm.name,
    price: dishForm.price,
    stock: dishForm.stock,
    status: dishForm.status,
    discountType: dishForm.discountType || null,
    discountValue: dishForm.discountType ? dishForm.discountValue : null,
  }

  const url =
    editingDishId.value != null
      ? api(`/api/merchant/${restaurantId.value}/dishes/${editingDishId.value}`)
      : api(`/api/merchant/${restaurantId.value}/dishes`)

  const method = editingDishId.value != null ? 'PUT' : 'POST'

  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  if (!res.ok) {
    msg.value = '保存菜品失败'
    return
  }
  resetDishForm()
  await loadAll()
}

async function deleteDish(dishId: number) {
  if (!restaurantId.value) return
  msg.value = ''
  if (!confirm('确定删除该菜品？')) return
  const res = await fetch(api(`/api/merchant/${restaurantId.value}/dishes/${dishId}`), {
    method: 'DELETE',
  })
  if (!res.ok) {
    msg.value = '删除菜品失败'
    return
  }
  await loadAll()
}

onMounted(loadAll)
</script>

<style scoped>
.page {
  padding: 0;
}

.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.warn {
  padding: 10px 12px;
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #614700;
  border-radius: 6px;
  margin-top: 12px;
}

.grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 12px;
  align-items: start;
}

.panel {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.hint {
  font-size: 12px;
  color: #666;
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

input,
select {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  font-size: 13px;
}

.btn {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.btn.primary {
  border-color: #1890ff;
  background-color: #1890ff;
  color: #fff;
}

.btn.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
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
  text-align: left;
}

.table thead {
  background-color: #fafafa;
}

.actions {
  display: flex;
  gap: 6px;
}

.empty {
  text-align: center;
  color: #999;
}

.msg {
  margin-top: 10px;
  color: #389e0d;
  font-size: 13px;
}
</style>

