<template>
  <nav class="game-bottom-tabs">
    <button
      v-for="item in navItems"
      :key="item.id"
      type="button"
      :class="{ active: activeTab === item.id }"
      @click="emit('change-tab', item.id)"
    >
      <span>{{ item.icon }}</span>{{ item.label }}
    </button>
    <button class="main-tab" type="button" @click="emit('open-float', 'profile')">+</button>
  </nav>
</template>

<script setup>
defineProps({
  activeTab: { type: String, required: true },
  navItems: { type: Array, required: true }
})

const emit = defineEmits(['change-tab', 'open-float'])
</script>
