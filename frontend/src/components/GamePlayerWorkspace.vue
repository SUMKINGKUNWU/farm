<template>
  <div class="game-page">
    <NoticeBlock :message="player.message" :error="player.error" :detail="player.errorDetail" />

    <section class="game-shell">
      <GameTopbar
        :player="player"
        :ready-count="readyCount"
        :format-money="formatMoney"
        :show-float="showFloat"
      />

      <div class="game-layout">
        <FarmRanchPanel
          :active-tab="activeTab"
          :player="player"
          :forms="forms"
          :seed-options="seedOptions"
          :animal-options="animalOptions"
          :current-market-item="currentMarketItem"
          :format-money="formatMoney"
          :format-duration="formatDuration"
          :slot-cells="slotCells"
          :is-ready="isReady"
          :slot-status-text="slotStatusText"
          :slot-action-text="slotActionText"
          :show-float="showFloat"
          :start-farm-production="startFarmProduction"
          :start-ranch-production="startRanchProduction"
          :harvest-growth="harvestGrowth"
        />

        <ShopPanel
          :active-tab="activeTab"
          :player="player"
          :forms="forms"
          :all-shop-options="allShopOptions"
          :show-float="showFloat"
          :purchase-shop-item="purchaseShopItem"
        />

        <MarketPanel
          :active-tab="activeTab"
          :player="player"
          :forms="forms"
          :harvest-options="harvestOptions"
          :current-market-item="currentMarketItem"
          :market-tax-estimate="marketTaxEstimate"
          :format-money="formatMoney"
          :show-float="showFloat"
          :submit-market="submitMarket"
        />

        <MarketSidebar :player="player" :forms="forms" :format-money="formatMoney" />

        <PrivateTradePanel
          :active-tab="activeTab"
          :player="player"
          :forms="forms"
          :harvest-options="harvestOptions"
          :private-tax-estimate="privateTaxEstimate"
          :format-money="formatMoney"
          :show-float="showFloat"
          :create-private-trade="createPrivateTrade"
          :accept-private-trade="acceptPrivateTrade"
        />
      </div>

      <GameBottomTabs
        :active-tab="activeTab"
        :nav-items="navItems"
        :set-active-tab="setActiveTab"
        :show-float="showFloat"
      />
    </section>

    <PlayerFloatLayer
      :active-float="activeFloat"
      :selected-info="selectedInfo"
      :player="player"
      :forms="forms"
      :market-tax-estimate="marketTaxEstimate"
      :private-tax-estimate="privateTaxEstimate"
      :format-money="formatMoney"
      :format-duration="formatDuration"
      :close-float="closeFloat"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import NoticeBlock from './common/NoticeBlock.vue'
import GameTopbar from './player/GameTopbar.vue'
import FarmRanchPanel from './player/FarmRanchPanel.vue'
import ShopPanel from './player/ShopPanel.vue'
import MarketPanel from './player/MarketPanel.vue'
import MarketSidebar from './player/MarketSidebar.vue'
import PrivateTradePanel from './player/PrivateTradePanel.vue'
import GameBottomTabs from './player/GameBottomTabs.vue'
import PlayerFloatLayer from './player/PlayerFloatLayer.vue'
import { usePlayerStore } from '../stores/playerStore'

const player = usePlayerStore()

const activeTab = ref('farm')
const activeFloat = ref('')
const selectedInfo = ref(null)

const forms = reactive({
  tradePassword: '',
  shopItemCode: 'WHEAT_SEED',
  shopQuantity: 1,
  shopTradePassword: '',
  farmItemCode: 'WHEAT_SEED',
  ranchItemCode: 'CHICKEN',
  marketItemCode: 'WHEAT',
  marketQuantity: 10,
  marketTradePassword: '',
  marketBulkTokenCode: '',
  privateBuyerUserId: '',
  privateItemCode: 'WHEAT',
  privateQuantity: 10,
  privatePriceAmount: 500,
  privateTradePassword: '',
  privateAcceptPassword: '',
  privateBulkTokenCode: ''
})

const seedOptions = computed(() => player.seeds.length ? player.seeds : [{ code: 'WHEAT_SEED', name: '小麦种子' }, { code: 'CORN_SEED', name: '玉米种子' }])
const animalOptions = computed(() => player.animals.length ? player.animals : [{ code: 'CHICKEN', name: '鸡苗' }, { code: 'COW', name: '奶牛' }])
const harvestOptions = computed(() => player.harvests.length ? player.harvests : [{ code: 'WHEAT', name: '小麦' }, { code: 'CORN', name: '玉米' }, { code: 'EGG', name: '鸡蛋' }, { code: 'MILK', name: '牛奶' }])
const readyCount = computed(() => player.activeGrowths.filter((growth) => isReady(growth)).length)
const allShopOptions = computed(() => [...seedOptions.value, ...animalOptions.value])
const currentMarketItem = computed(() => harvestOptions.value.find((item) => item.code === forms.marketItemCode) || harvestOptions.value[0])
const marketTaxEstimate = computed(() => {
  const price = Number(player.quote?.currentPrice || 0)
  const quantity = Number(forms.marketQuantity || 0)
  return Math.round(price * quantity * 0.03)
})
const privateTaxEstimate = computed(() => Math.round(Number(forms.privatePriceAmount || 0) * 0.05))
const navItems = [
  { id: 'farm', label: '农场', icon: '田' },
  { id: 'shop', label: '商店', icon: '仓' },
  { id: 'market', label: '交易站', icon: '市' },
  { id: 'private', label: '私下', icon: '约' }
]

function setActiveTab(tab) {
  activeTab.value = tab
}

function formatMoney(value) {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN')
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

function formatDuration(seconds) {
  if (!seconds) return '-'
  if (seconds < 3600) return `${Math.ceil(seconds / 60)} 分钟`
  return `${(seconds / 3600).toFixed(1)} 小时`
}

function growthBySlot(slotId) {
  return player.activeGrowths.find((growth) => growth.slotId === slotId)
}

function isReady(growth) {
  return Boolean(growth && new Date(growth.readyAt).getTime() <= Date.now())
}

function slotCells(slotList) {
  const slots = slotList?.slots || []
  const maxSlots = slotList?.maxSlots || 16
  return Array.from({ length: maxSlots }, (_, index) => {
    const slot = slots[index]
    return {
      index: index + 1,
      unlocked: Boolean(slot),
      slot,
      growth: slot ? growthBySlot(slot.id) : null
    }
  })
}

function slotStatusText(cell) {
  if (!cell?.unlocked) return '未扩建'
  if (!cell.growth) return '可投入'
  return isReady(cell.growth) ? '可收获' : '成长中'
}

function slotActionText(cell, emptyText) {
  if (!cell?.unlocked) return '先扩建栏位'
  if (!cell.growth) return emptyText
  return isReady(cell.growth) ? '点击收获' : formatDate(cell.growth.readyAt)
}

function showFloat(type, info = null) {
  selectedInfo.value = info
  activeFloat.value = type
}

function closeFloat() {
  activeFloat.value = ''
  selectedInfo.value = null
}

function handleKeydown(event) {
  if (event.key === 'Escape' && activeFloat.value) {
    closeFloat()
  }
}

window.addEventListener('keydown', handleKeydown)

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})

async function purchaseShopItem() {
  await player.purchase({
    itemCode: forms.shopItemCode,
    quantity: forms.shopQuantity,
    tradePassword: forms.shopTradePassword
  })
}

async function startFarmProduction(slotId) {
  await player.startProduction('FARM', slotId, forms.farmItemCode)
}

async function startRanchProduction(slotId) {
  await player.startProduction('RANCH', slotId, forms.ranchItemCode)
}

async function harvestGrowth(growthId) {
  await player.harvest(growthId)
}

async function submitMarket(side) {
  await player.marketTrade(side, {
    itemCode: forms.marketItemCode,
    quantity: forms.marketQuantity,
    tradePassword: forms.marketTradePassword,
    bulkTokenCode: forms.marketBulkTokenCode || null
  })
  showFloat('trade', { side })
}

async function createPrivateTrade() {
  await player.createPrivateTrade({
    buyerUserId: forms.privateBuyerUserId,
    itemCode: forms.privateItemCode,
    quantity: forms.privateQuantity,
    priceAmount: forms.privatePriceAmount,
    tradePassword: forms.privateTradePassword
  })
}

async function acceptPrivateTrade(offerId) {
  await player.acceptPrivateTrade(offerId, {
    tradePassword: forms.privateAcceptPassword,
    bulkTokenCode: forms.privateBulkTokenCode || null
  })
}
</script>
