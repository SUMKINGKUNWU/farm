<template>
  <section id="tax" class="panel tax-panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Tax Config</span>
        <h3>税率配置</h3>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('load-tax-configs')">
        读取税率
      </button>
    </div>
    <div class="tax-grid">
      <article v-for="type in adminTaxTypes" :key="type.code" class="tax-card">
        <div>
          <span>{{ type.name }}</span>
          <strong>{{ formatRate(taxForm[type.code].rateBasisPoints) }}</strong>
        </div>
        <label>
          Basis Points
          <input v-model.number="taxForm[type.code].rateBasisPoints" type="number" min="0" max="5000" />
        </label>
        <label>
          调整原因
          <input v-model.trim="taxForm[type.code].reason" placeholder="例如：活动期降低交易站税率" />
        </label>
        <button class="button" type="button" :disabled="loading" @click="emit('save-tax', type.code)">
          保存 {{ type.name }}
        </button>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  adminTaxTypes: { type: Array, required: true },
  taxForm: { type: Object, required: true },
  loading: { type: Boolean, required: true },
  formatRate: { type: Function, required: true }
})

const emit = defineEmits(['load-tax-configs', 'save-tax'])
</script>
