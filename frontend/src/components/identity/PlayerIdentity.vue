<template>
  <div class="identity-card">
    <template v-if="!player.isLoggedIn">
      <label>玩家用户名<input v-model.trim="player.username" placeholder="例如 farmer_001" /></label>
      <label>玩家昵称<input v-model.trim="player.nickname" placeholder="注册时填写昵称" /></label>
      <label>
        登录密码
        <input v-model="player.password" type="password" placeholder="至少 6 位" @keydown.enter="player.login" />
      </label>
      <div class="button-row">
        <button class="button ghost" type="button" :disabled="player.loading" @click="player.login">登录</button>
        <button class="button subtle" type="button" :disabled="player.loading" @click="player.register">注册并登录</button>
      </div>
    </template>
    <template v-else>
      <div class="login-badge">
        <span>当前玩家</span>
        <strong>{{ player.currentUser?.username || player.username || '已登录' }}</strong>
      </div>
      <button class="button ghost" type="button" :disabled="player.loading" @click="player.loadDashboard">刷新农场</button>
      <button class="button subtle" type="button" @click="player.logout">退出登录</button>
    </template>
  </div>
</template>

<script setup>
defineProps({
  player: { type: Object, required: true }
})
</script>
