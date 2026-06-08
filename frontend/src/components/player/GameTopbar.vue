<template>
  <div class="game-topbar">
    <button class="avatar-button" type="button" @click="emit('open-float', 'profile')">
      <span class="avatar-seed">穗</span>
      <span>
        <b>{{ player.currentUser?.nickname || player.currentUser?.username || player.username || '农场主' }}</b>
        <small>点击查看个人信息</small>
      </span>
    </button>
    <div class="wallet-strip">
      <span>金币</span>
      <strong>{{ formatMoney(player.summary?.balance ?? 10000) }}</strong>
    </div>
    <div class="wallet-strip muted-strip">
      <span>可收获</span>
      <strong>{{ readyCount }}</strong>
    </div>
    <button class="round-action" type="button" :disabled="player.loading" @click="emit('refresh')">刷新</button>
  </div>
</template>

<script setup>
defineProps({
  player: { type: Object, required: true },
  readyCount: { type: Number, required: true },
  formatMoney: { type: Function, required: true }
})

const emit = defineEmits(['open-float', 'refresh'])
</script>
