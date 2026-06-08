<template>
  <div class="identity-card">
    <template v-if="!admin.isLoggedIn">
      <label>管理员用户名<input v-model.trim="admin.username" placeholder="请输入管理员用户名" /></label>
      <label>
        登录密码
        <input v-model="admin.password" type="password" placeholder="请输入登录密码" @keydown.enter="admin.login" />
      </label>
      <button class="button ghost" type="button" :disabled="admin.loading" @click="admin.login">登录管理台</button>
    </template>
    <template v-else>
      <div class="login-badge">
        <span>当前管理员</span>
        <strong>{{ admin.currentUser?.username || admin.username || '已登录' }}</strong>
      </div>
      <label>目标玩家用户 ID<input v-model.trim="admin.targetUserId" placeholder="要查询或发令牌的玩家 UUID" /></label>
      <button class="button ghost" type="button" :disabled="admin.loading" @click="emit('refresh-console')">
        刷新控制台
      </button>
      <button class="button subtle" type="button" @click="admin.logout">退出登录</button>
    </template>
  </div>
</template>

<script setup>
defineProps({
  admin: { type: Object, required: true }
})

const emit = defineEmits(['refresh-console'])
</script>
