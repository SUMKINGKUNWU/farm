<template>
  <nav class="game-bottom-tabs">
    <button
      v-for="item in navItems"
      :key="item.id"
      type="button"
      :class="{ active: activeTab === item.id }"
      @click="setActiveTab(item.id)"
    >
      <span>{{ item.icon }}</span>{{ item.label }}
    </button>
    <button class="main-tab" type="button" @click="showFloat('profile')">+</button>
  </nav>
</template>

<script setup>
defineProps({
  activeTab: { type: String, required: true },
  navItems: { type: Array, required: true },
  setActiveTab: { type: Function, required: true },
  showFloat: { type: Function, required: true }
})
</script>
