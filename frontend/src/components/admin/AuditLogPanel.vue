<template>
  <section id="audits" class="panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Audit Trail</span>
        <h3>审计日志</h3>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('load-audit-logs')">
        刷新日志
      </button>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>时间</th><th>管理员</th><th>动作</th><th>目标</th><th>原因</th></tr>
        </thead>
        <tbody>
          <tr v-for="entry in auditLogs" :key="entry.auditId">
            <td>{{ formatDate(entry.createdAt) }}</td>
            <td>{{ entry.adminUsername || '-' }}<small>{{ entry.adminUserId || '-' }}</small></td>
            <td>{{ entry.action }}</td>
            <td>{{ entry.targetType || '-' }}<small>{{ entry.targetId || '-' }}</small></td>
            <td>{{ entry.reason || '-' }}</td>
          </tr>
          <tr v-if="!auditLogs.length"><td colspan="5">暂无审计日志</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
defineProps({
  auditLogs: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['load-audit-logs'])
</script>
