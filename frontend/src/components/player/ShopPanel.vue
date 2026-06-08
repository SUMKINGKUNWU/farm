<template>
  <section v-show="activeTab === 'shop'" class="farm-canvas">
    <div class="canvas-heading">
      <div><span class="section-kicker">Shop</span><h2>农场商店</h2></div>
      <button class="button ghost" type="button" @click="emit('open-float', 'item', allShopOptions[0])">查看商品浮层</button>
    </div>
    <div class="shop-layout">
      <form class="game-card-form" @submit.prevent="emit('purchase')">
        <div v-if="!player.summary?.tradePasswordSet" class="requirement-hint">
          <strong>需要先设置交易密码</strong>
          <span>购买种子或动物前，请点底部中间按钮打开个人信息并设置 6 位交易密码。</span>
        </div>
        <label>
          商品
          <select v-model="forms.shopItemCode">
            <option v-for="item in allShopOptions" :key="item.code" :value="item.code">{{ item.name }} · {{ item.code }}</option>
          </select>
        </label>
        <label>数量<input v-model.number="forms.shopQuantity" type="number" min="1" /></label>
        <label>交易密码<input v-model="forms.shopTradePassword" type="password" placeholder="6 位数字" /></label>
        <button class="button wide" type="submit" :disabled="player.loading">购买入库</button>
      </form>
      <div class="inventory-cloud">
        <h3>库存</h3>
        <button v-for="item in player.inventory" :key="item.itemId" type="button" class="inventory-pill" @click="emit('open-float', 'item', item)">
          <span>{{ item.itemName }}</span><strong>{{ item.availableQuantity }}</strong>
        </button>
        <div v-if="!player.inventory.length" class="empty-action">
          <strong>库存为空</strong>
          <span>先在左侧选择种子或动物并购买，随后回到农场投入生产。</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  activeTab: { type: String, required: true },
  player: { type: Object, required: true },
  forms: { type: Object, required: true },
  allShopOptions: { type: Array, required: true }
})

const emit = defineEmits(['open-float', 'purchase'])
</script>
