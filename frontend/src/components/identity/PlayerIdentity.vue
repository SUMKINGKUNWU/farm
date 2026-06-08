<template>
  <div class="identity-card">
    <template v-if="!isLoggedIn">
      <label>玩家用户名<input v-model.trim="identityForm.username" placeholder="例如 farmer_001" /></label>
      <label>玩家昵称<input v-model.trim="identityForm.nickname" placeholder="注册时填写昵称" /></label>
      <label>
        登录密码
        <input v-model="identityForm.password" type="password" placeholder="至少 6 位" @keydown.enter="emit('login')" />
      </label>
      <div class="button-row">
        <button class="button ghost" type="button" :disabled="loading" @click="emit('login')">登录</button>
        <button class="button subtle" type="button" :disabled="loading" @click="emit('register')">注册并登录</button>
      </div>
    </template>
    <template v-else>
      <div class="login-badge">
        <span>当前玩家</span>
        <strong>{{ currentUser?.username || identityForm.username || '已登录' }}</strong>
      </div>
      <button class="button ghost" type="button" :disabled="loading" @click="emit('refresh-dashboard')">刷新农场</button>
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

const emit = defineEmits(['login', 'register', 'refresh-dashboard', 'logout'])
</script>
