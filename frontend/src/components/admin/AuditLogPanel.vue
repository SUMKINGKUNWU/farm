<template>
  <section id="audits" class="panel">
    <div class="panel-heading">
      <div>
        <span class="section-kicker">Audit Trail</span>
        <h3>审计日志</h3>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="refreshCurrentPage">
        刷新日志
      </button>
    </div>

    <div class="toolbar-row">
      <label>
        动作
        <select v-model="draftFilters.action">
          <option value="ALL">全部</option>
          <option v-for="option in actionOptions" :key="option" :value="option">
            {{ adminAuditActionLabel(option) }}
          </option>
        </select>
      </label>
      <label>
        目标
        <select v-model="draftFilters.targetType">
          <option value="ALL">全部</option>
          <option v-for="option in targetTypeOptions" :key="option" :value="option">
            {{ adminAuditTargetTypeLabel(option) }}
          </option>
        </select>
      </label>
      <label>
        原因建议
        <select :value="selectedReasonOption" @change="applyReasonOption($event.target.value)">
          <option value="">不限</option>
          <option v-for="option in reasonOptions" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
      </label>
      <label>
        原因关键词
        <input
          v-model.trim="draftFilters.reason"
          type="text"
          list="audit-reason-options"
          placeholder="可手填关键词，或先选建议项"
        />
        <datalist id="audit-reason-options">
          <option v-for="option in reasonOptions" :key="option" :value="option" />
        </datalist>
      </label>
      <label>
        开始日期
        <input v-model="draftFilters.from" type="date" />
      </label>
      <label>
        结束日期
        <input v-model="draftFilters.to" type="date" />
      </label>
      <label>
        每页
        <select v-model.number="draftFilters.pageSize">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </label>
      <button class="button subtle" type="button" :disabled="loading" @click="applyFilters">
        应用筛选
      </button>
      <button class="button ghost" type="button" :disabled="loading" @click="resetFilters">
        重置日志
      </button>
    </div>

    <div class="table-meta">
      <span>共 {{ auditResult.total }} 条</span>
      <span>第 {{ auditResult.page }} / {{ totalPages }} 页</span>
    </div>
    <div class="filter-summary">
      <strong>当前审计条件</strong>
      <span v-if="summaryItems.length">{{ summaryItems.join(' / ') }}</span>
      <span v-else>默认条件</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>时间</th>
            <th>管理员</th>
            <th>动作</th>
            <th>目标</th>
            <th>原因</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in auditResult.records" :key="entry.auditId">
            <td>{{ formatDate(entry.createdAt) }}</td>
            <td>{{ entry.adminUsername || '-' }}<small>{{ entry.adminUserId || '-' }}</small></td>
            <td>{{ adminAuditActionLabel(entry.action) }}</td>
            <td>{{ adminAuditTargetTypeLabel(entry.targetType) }}<small>{{ entry.targetId || '-' }}</small></td>
            <td>{{ entry.reason || '-' }}</td>
          </tr>
          <tr v-if="!auditResult.records.length">
            <td colspan="5">暂无审计日志</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager-row">
      <button class="button ghost" type="button" :disabled="loading || auditResult.page <= 1" @click="goPrev">
        上一页
      </button>
      <label class="page-jump">
        页码
        <input v-model.number="jumpPage" type="number" min="1" :max="totalPages" />
      </label>
      <button class="button subtle" type="button" :disabled="loading" @click="goToPage">
        跳转
      </button>
      <button class="button ghost" type="button" :disabled="loading || !auditResult.hasNext" @click="goNext">
        下一页
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { adminAuditActionLabel, adminAuditTargetTypeLabel } from '../../utils/adminLabels'

const props = defineProps({
  auditResult: { type: Object, required: true },
  auditFilters: { type: Object, required: true },
  auditFilterOptions: {
    type: Object,
    default: () => ({
      actionOptions: [],
      targetTypeOptions: [],
      reasonOptions: []
    })
  },
  loading: { type: Boolean, required: true },
  formatDate: { type: Function, required: true }
})

const emit = defineEmits(['load-audit-logs'])
const jumpPage = ref(1)
const defaultDate = new Date().toISOString().slice(0, 10)

const defaultDraft = () => ({
  action: 'ALL',
  targetType: 'ALL',
  reason: '',
  from: defaultDate,
  to: defaultDate,
  pageSize: 10
})

const draftFilters = reactive(defaultDraft())

const totalPages = computed(() => {
  const size = Math.max(Number(props.auditResult.pageSize || props.auditFilters.pageSize || 10), 1)
  return Math.max(1, Math.ceil(Number(props.auditResult.total || 0) / size))
})

const actionOptions = computed(() => props.auditFilterOptions.actionOptions || [])
const targetTypeOptions = computed(() => props.auditFilterOptions.targetTypeOptions || [])
const reasonOptions = computed(() => props.auditFilterOptions.reasonOptions || [])

const selectedReasonOption = computed(() => {
  return reasonOptions.value.includes(draftFilters.reason) ? draftFilters.reason : ''
})

const summaryItems = computed(() => {
  const items = []
  if (props.auditFilters.action !== 'ALL') {
    items.push(`动作：${adminAuditActionLabel(props.auditFilters.action)}`)
  }
  if (props.auditFilters.targetType !== 'ALL') {
    items.push(`目标：${adminAuditTargetTypeLabel(props.auditFilters.targetType)}`)
  }
  if (props.auditFilters.reason) {
    items.push(`原因：${props.auditFilters.reason}`)
  }
  if (props.auditFilters.from && props.auditFilters.from !== defaultDate) {
    items.push(`开始：${props.auditFilters.from}`)
  }
  if (props.auditFilters.to && props.auditFilters.to !== defaultDate) {
    items.push(`结束：${props.auditFilters.to}`)
  }
  if (Number(props.auditFilters.pageSize) !== 10) {
    items.push(`每页：${props.auditFilters.pageSize}`)
  }
  return items
})

watch(
  () => props.auditFilters,
  (filters) => {
    draftFilters.action = filters.action
    draftFilters.targetType = filters.targetType
    draftFilters.reason = filters.reason || ''
    draftFilters.from = filters.from
    draftFilters.to = filters.to
    draftFilters.pageSize = filters.pageSize
  },
  { deep: true, immediate: true }
)

watch(
  () => props.auditResult.page,
  (page) => {
    jumpPage.value = page
  },
  { immediate: true }
)

function applyReasonOption(value) {
  draftFilters.reason = value || ''
}

function applyFilters() {
  emit('load-audit-logs', {
    action: draftFilters.action,
    targetType: draftFilters.targetType,
    reason: draftFilters.reason,
    from: draftFilters.from,
    to: draftFilters.to,
    pageSize: draftFilters.pageSize,
    page: 1
  })
}

function resetFilters() {
  Object.assign(draftFilters, defaultDraft())
  emit('load-audit-logs', {
    ...defaultDraft(),
    page: 1
  })
}

function refreshCurrentPage() {
  emit('load-audit-logs', { page: props.auditResult.page })
}

function goPrev() {
  emit('load-audit-logs', { page: props.auditResult.page - 1 })
}

function goNext() {
  emit('load-audit-logs', { page: props.auditResult.page + 1 })
}

function goToPage() {
  const page = Math.min(Math.max(Number(jumpPage.value || 1), 1), totalPages.value)
  emit('load-audit-logs', { page })
}
</script>
