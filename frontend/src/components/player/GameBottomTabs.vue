<template>
  <nav class="game-bottom-tabs">
    <button
      v-for="item in allNavItems"
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
const props = defineProps({
  activeTab: { type: String, required: true },
  navItems: { type: Array, required: true }
})

const allNavItems = [
  ...props.navItems,
  { id: 'records', label: '记录', icon: '簿' }
]

const emit = defineEmits(['change-tab', 'open-float'])
</script>
