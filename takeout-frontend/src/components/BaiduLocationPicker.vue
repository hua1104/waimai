<template>
  <div class="wrap" :class="{ compact: !!props.compact }">
    <div v-if="!props.compact" class="row">
      <input v-model="query" class="query" placeholder="输入地址关键字（如：山东大学中心校区）" />
      <button class="btn" :disabled="!ak" @click="search">搜索</button>
      <button class="btn" :disabled="!ak" @click="open">地图选点</button>
      <button class="btn" :disabled="!ak" @click="useGeo">使用当前位置</button>
    </div>
    <div v-else class="compact-row">
      <input v-model="query" class="compact-query" placeholder="搜索地点（如：xx小区）" />
      <button class="btn" :disabled="!ak || !query.trim()" @click="search">搜索</button>
      <button class="btn" :disabled="!ak" @click="open">地图选点</button>
      <button class="btn" :disabled="!ak" @click="useGeo">使用当前位置</button>
      <button v-if="selected" class="btn" @click="emitClear">清除定位</button>
    </div>
    <div v-if="!ak" class="hint">
      未配置百度地图 Key：请在 `takeout-frontend/.env` 添加 `VITE_BAIDU_MAP_AK=你的AK` 后重启前端。
    </div>
    <div v-if="loadErr" class="hint err">{{ loadErr }}</div>
    <div v-if="selected" class="selected" :class="{ compact: !!props.compact }">
      <span class="tag ok">已选</span>
      <span class="text">{{ selected.address }}</span>
      <span class="coord">({{ selected.lat.toFixed(6) }}, {{ selected.lng.toFixed(6) }})</span>
      <button v-if="!props.compact" class="mini" @click="emitClear">清除</button>
    </div>

    <div v-if="visible" class="mask" @click.self="close">
      <div class="modal">
        <div class="modal-head">
          <div class="title">地图选点</div>
          <button class="mini" @click="close">关闭</button>
        </div>
        <div class="tip">点击地图选择收货点；会自动反查地址。</div>
        <div class="map-wrap">
          <div v-if="loadingMap" class="map-loading">地图加载中...</div>
          <div ref="mapRef" class="map"></div>
        </div>
        <div class="modal-foot">
          <button class="btn primary" :disabled="!picked" @click="confirm">确认使用该位置</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'

type Picked = { address: string; lat: number; lng: number }

const props = defineProps<{
  modelValue: Picked | null
  compact?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: Picked | null): void
}>()

const ak = (import.meta as any).env?.VITE_BAIDU_MAP_AK as string | undefined
const hasAk = computed(() => !!ak && String(ak).trim().length > 0)

const selected = computed(() => props.modelValue)
const query = ref('')

const visible = ref(false)
const mapRef = ref<HTMLDivElement | null>(null)
const picked = ref<Picked | null>(null)
const loadingMap = ref(false)
const loadErr = ref('')

let map: any = null
let marker: any = null
let clickHandler: any = null

function emitClear() {
  emit('update:modelValue', null)
  picked.value = null
}

function close() {
  visible.value = false
}

function ensureBMap(): Promise<void> {
  if (!hasAk.value) return Promise.reject(new Error('missing ak'))
  const w = window as any
  if (w.BMap) return Promise.resolve()

  return new Promise((resolve, reject) => {
    const cbName = '__baiduMapLoaded__'
    w[cbName] = () => resolve()
    const script = document.createElement('script')
    script.src = `https://api.map.baidu.com/api?v=3.0&ak=${encodeURIComponent(String(ak))}&callback=${cbName}`
    script.async = true
    script.onerror = () => reject(new Error('load baidu map failed'))
    document.head.appendChild(script)
  })
}

async function open() {
  if (!hasAk.value) return
  picked.value = null
  loadErr.value = ''
  loadingMap.value = true
  visible.value = true
  await nextTick()
  try {
    await ensureBMap()
  } catch {
    loadErr.value =
      '百度地图加载失败：请确认已开通“Javascript API / jsapi底图 / JS逆地理编码”，并设置 Referer 白名单为 localhost:5173*'
    loadingMap.value = false
    return
  }
  await nextTick()
  initMap()
  loadingMap.value = false
}

function initMap() {
  const w = window as any
  if (!mapRef.value || !w.BMap) return

  map = new w.BMap.Map(mapRef.value)
  const center = selected.value
    ? new w.BMap.Point(selected.value.lng, selected.value.lat)
    : new w.BMap.Point(116.404, 39.915) // 默认北京
  map.centerAndZoom(center, selected.value ? 16 : 12)
  map.enableScrollWheelZoom(true)

  if (selected.value) {
    marker = new w.BMap.Marker(center)
    map.addOverlay(marker)
  }

  const geocoder = new w.BMap.Geocoder()

  clickHandler = (e: any) => {
    const pt = e.point
    if (!pt) return
    if (marker) {
      map.removeOverlay(marker)
      marker = null
    }
    marker = new w.BMap.Marker(pt)
    map.addOverlay(marker)
    map.panTo(pt)

    geocoder.getLocation(pt, (rs: any) => {
      const comp = rs?.addressComponents
      const addr = rs?.address ?? (comp ? `${comp.province}${comp.city}${comp.district}${comp.street}${comp.streetNumber}` : '')
      picked.value = {
        address: addr || query.value || '已选位置',
        lat: Number(pt.lat),
        lng: Number(pt.lng),
      }
    })
  }

  map.addEventListener('click', clickHandler)
}

async function search() {
  if (!hasAk.value) return
  loadErr.value = ''
  try {
    await ensureBMap()
  } catch {
    loadErr.value =
      '百度地图加载失败：请确认已开通“Javascript API / jsapi底图 / JS逆地理编码”，并设置 Referer 白名单为 localhost:5173*'
    return
  }

  if (!query.value.trim()) {
    loadErr.value = '请输入关键字后再搜索'
    return
  }

  if (!visible.value) {
    await open()
    if (!visible.value) return
  }
  const w = window as any
  if (!map || !w.BMap) return

  const local = new w.BMap.LocalSearch(map, {
    onSearchComplete: (results: any) => {
      if (!results || results.getCurrentNumPois?.() <= 0) {
        loadErr.value = '未找到匹配地点（可尝试更详细的关键字）'
        return
      }
      const poi = results.getPoi(0)
      if (!poi?.point) {
        loadErr.value = '搜索结果无坐标'
        return
      }
      map.centerAndZoom(poi.point, 16)
      if (marker) {
        map.removeOverlay(marker)
        marker = null
      }
      marker = new w.BMap.Marker(poi.point)
      map.addOverlay(marker)
      picked.value = {
        address: poi.address ? `${poi.title}（${poi.address}）` : poi.title,
        lat: Number(poi.point.lat),
        lng: Number(poi.point.lng),
      }
    },
  })
  local.search(query.value.trim())
}

async function useGeo() {
  if (!hasAk.value) return
  const w = window as any
  loadErr.value = ''
  try {
    await ensureBMap()
  } catch {
    loadErr.value =
      '百度地图加载失败：请确认已开通“Javascript API / jsapi底图 / JS逆地理编码”，并设置 Referer 白名单为 localhost:5173*'
    return
  }
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const rawLat = pos.coords.latitude
      const rawLng = pos.coords.longitude

      const convertor = new w.BMap.Convertor()
      const rawPt = new w.BMap.Point(rawLng, rawLat)
      convertor.translate([rawPt], 1, 5, (data: any) => {
        const pt = data?.points?.[0] ?? rawPt
        const geocoder = new w.BMap.Geocoder()
        geocoder.getLocation(pt, (rs: any) => {
          const addr = rs?.address ?? '当前位置'
          emit('update:modelValue', { address: addr, lat: Number(pt.lat), lng: Number(pt.lng) })
        })
      })
    },
    () => {},
    { enableHighAccuracy: true, timeout: 8000 },
  )
}

function confirm() {
  if (!picked.value) return
  emit('update:modelValue', picked.value)
  visible.value = false
}

onBeforeUnmount(() => {
  try {
    if (map && clickHandler) map.removeEventListener('click', clickHandler)
  } catch {}
})
</script>

<style scoped>
.wrap {
  display: grid;
  gap: 8px;
}

.wrap.compact {
  gap: 6px;
}

.row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.query {
  flex: 1;
  min-width: 220px;
}

.compact-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 8px;
  align-items: center;
}

.compact-query {
  min-width: 160px;
}

.selected.compact .coord {
  display: none;
}

.selected.compact .text {
  max-width: 80vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .compact-row {
    grid-template-columns: 1fr 1fr;
  }

  .compact-query {
    grid-column: 1 / -1;
  }
}

.hint {
  font-size: 12px;
  color: #666;
}

.hint.err {
  color: #ff4d4f;
}

.selected {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
}

.selected.compact {
  gap: 6px;
  font-size: 12px;
}

.coord {
  color: #666;
  font-size: 12px;
}

.mini {
  font-size: 12px;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 1000;
}

.modal {
  width: min(900px, 100%);
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.title {
  font-weight: 700;
}

.tip {
  padding: 10px 12px;
  font-size: 12px;
  color: #666;
}

.map {
  height: 480px;
}

.map-wrap {
  position: relative;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #666;
  font-size: 13px;
  z-index: 1;
}

.modal-foot {
  padding: 10px 12px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}
</style>
