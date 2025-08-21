<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo text-2xl font-bold text-primary">AI Creation</div>
      <nav class="nav">
        <router-link to="/users" class="nav-link">用户管理</router-link>
        <router-link to="/articles" class="nav-link">文章管理</router-link>
        <router-link to="/dashboard" class="nav-link">仪表盘</router-link>
      </nav>
    </aside>
    
    <main class="main">
      <header class="header">
        <h1 class="text-xl font-semibold text-text">管理后台</h1>
        <div class="flex items-center gap-4">
          <span class="text-text-secondary">欢迎，{{ userName }}</span>
          <button @click="logout" class="btn btn-outline btn-sm">退出登录</button>
        </div>
      </header>
      
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();
const userName = computed(() => auth.userName);

const logout = () => {
  auth.logout();
  router.push('/login');
};
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #fff7ed 0%, #ffedd5 100%);
  border-right: 1px solid var(--border);
  padding: 1.5rem 1rem;
}

.logo {
  margin-bottom: 2rem;
  text-align: center;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.nav-link {
  display: block;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  font-weight: 500;
}

.nav-link:hover {
  background: var(--surface-hover);
  color: var(--primary);
}

.nav-link.router-link-active {
  background: var(--primary);
  color: white;
}

.main {
  background: var(--background);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border);
  background: white;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: var(--shadow);
}

.content {
  padding: 1.5rem;
}
</style>


