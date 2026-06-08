<template>
  <div class="identity-card">
    <template v-if="!isLoggedIn">
      <label>管理员用户名<input v-model.trim="identityForm.username" placeholder="请输入管理员用户名" /></label>
      <label>
        登录密码
        <input v-model="identityForm.password" type="password" placeholder="请输入登录密码" @keydown.enter="emit('login')" />
      </label>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('login')">登录管理台</button>
    </template>
    <template v-else>
      <div class="login-badge">
        <span>当前管理员</span>
        <strong>{{ currentUser?.username || identityForm.username || '已登录' }}</strong>
      </div>
      <label>目标玩家用户 ID<input v-model.trim="identityForm.targetUserId" placeholder="要查询或发令牌的玩家 UUID" /></label>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('refresh-console')">
        刷新控制台
      </button>
      <button class="button subtle" type="button" @click="emit('logout')">退出登录</button>
    </template>
  </div>
</template>

<script setup>
defineProps({
  identityForm: { type: Object, required: true },
  isLoggedIn: { type: Boolean, required: true },
  currentUser: { type: Object, default: null },
  loading: { type: Boolean, required: true }
})

const emit = defineEmits(['login', 'refresh-console', 'logout'])
</script>
