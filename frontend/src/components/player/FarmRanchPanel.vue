<template>
  <section v-show="activeTab === 'farm'" class="farm-canvas">
    <div class="canvas-heading">
      <div>
        <span class="section-kicker">Farm & Ranch</span>
        <h2>农田与牧场</h2>
      </div>
      <div class="expand-actions">
        <button class="button ghost" type="button" :disabled="loading" @click="emit('expand', 'FARM')">
          扩建农田 {{ formatMoney(summary?.nextFarmExpandCost) }}
        </button>
        <button class="button ghost" type="button" :disabled="loading" @click="emit('expand', 'RANCH')">
          扩建牧场 {{ formatMoney(summary?.nextRanchExpandCost) }}
        </button>
      </div>
    </div>

    <div class="production-toolbar game-toolbar">
      <label>
        农田种子
        <select v-model="forms.farmItemCode">
          <option v-for="item in seedOptions" :key="item.code" :value="item.code">
            {{ item.name }} · {{ formatDuration(item.growSeconds) }}
          </option>
        </select>
      </label>
      <label>
        牧场动物
        <select v-model="forms.ranchItemCode">
          <option v-for="item in animalOptions" :key="item.code" :value="item.code">
            {{ item.name }} · {{ formatDuration(item.growSeconds) }}
          </option>
        </select>
      </label>
      <button class="button subtle-light" type="button" @click="emit('open-float', 'item', currentMarketItem)">物品信息</button>
    </div>

    <div class="game-field-board">
      <article class="game-zone">
        <div class="zone-heading"><span>Farm Plots</span><strong>农田 {{ summary?.farmSlots || 0 }}/16</strong></div>
        <div class="game-slot-grid">
          <button
            v-for="cell in slotCells(farm)"
            :key="'game-farm-' + cell.index"
            class="game-slot field-slot"
            :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }"
            type="button"
            :disabled="loading || !cell.unlocked"
            @click="cell.growth ? emit('harvest', cell.growth.growthId) : emit('start-production', { type: 'FARM', slotId: cell.slot.id })"
          >
            <span class="slot-top"><i>#{{ cell.index }}</i><b>{{ slotStatusText(cell) }}</b></span>
            <strong>{{ !cell.unlocked ? '未扩建' : cell.growth ? cell.growth.outputItemCode : '空闲' }}</strong>
            <small>{{ slotActionText(cell, '点击播种') }}</small>
          </button>
        </div>
      </article>
      <article class="game-zone ranch-game-zone">
        <div class="zone-heading"><span>Ranch Slots</span><strong>牧场 {{ summary?.ranchSlots || 0 }}/16</strong></div>
        <div class="game-slot-grid">
          <button
            v-for="cell in slotCells(ranch)"
            :key="'game-ranch-' + cell.index"
            class="game-slot ranch-slot"
            :class="{ locked: !cell.unlocked, busy: cell.growth, ready: isReady(cell.growth) }"
            type="button"
            :disabled="loading || !cell.unlocked"
            @click="cell.growth ? emit('harvest', cell.growth.growthId) : emit('start-production', { type: 'RANCH', slotId: cell.slot.id })"
          >
            <span class="slot-top"><i>#{{ cell.index }}</i><b>{{ slotStatusText(cell) }}</b></span>
            <strong>{{ !cell.unlocked ? '未扩建' : cell.growth ? cell.growth.outputItemCode : '空闲' }}</strong>
            <small>{{ slotActionText(cell, '点击养殖') }}</small>
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  activeTab: { type: String, required: true },
  summary: { type: Object, default: null },
  farm: { type: Object, default: null },
  ranch: { type: Object, default: null },
  loading: { type: Boolean, required: true },
  forms: { type: Object, required: true },
  seedOptions: { type: Array, required: true },
  animalOptions: { type: Array, required: true },
  currentMarketItem: { type: Object, default: null },
  formatMoney: { type: Function, required: true },
  formatDuration: { type: Function, required: true },
  slotCells: { type: Function, required: true },
  isReady: { type: Function, required: true },
  slotStatusText: { type: Function, required: true },
  slotActionText: { type: Function, required: true }
})

const emit = defineEmits(['expand', 'open-float', 'start-production', 'harvest'])
</script>
