<template>
  <div>
    <div v-if="message" class="notice success">{{ message }}</div>
    <div v-if="error" class="notice error">
      <div class="notice-title">
        <span>{{ error }}</span>
        <code v-if="detail?.code">{{ detail.code }}</code>
      </div>
      <p v-if="detail?.action">{{ detail.action }}</p>
      <p v-if="detail?.status || detail?.path" class="notice-meta">HTTP {{ detail.status || '-' }} · {{ detail.path || '本地校验' }}</p>
      <ul v-if="detail?.fieldErrors?.length" class="field-errors">
        <li v-for="fieldError in detail.fieldErrors" :key="`${fieldError.field}-${fieldError.message}`">
          {{ fieldError.field }}：{{ fieldError.message }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' },
  error: { type: String, default: '' },
  detail: { type: Object, default: null }
})
</script>
