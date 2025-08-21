<template>
  <div class="center-page">
    <div class="auth-card card">
      <div class="card-header text-center">
        <h1 class="text-2xl font-bold text-primary mb-2">AI Creation</h1>
        <p class="text-text-secondary">欢迎登录管理后台</p>
      </div>
      
      <div class="card-body">
        <form @submit.prevent="handleLogin" class="space-y-4">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
              v-model="userName"
              type="text"
              class="form-input"
              placeholder="请输入用户名"
              required
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">密码</label>
            <input
              v-model="password"
              type="password"
              class="form-input"
              placeholder="请输入密码"
              required
            />
          </div>
          
          <div v-if="error" class="alert alert-error">
            {{ error }}
          </div>
          
          <button
            type="submit"
            class="btn btn-primary w-full"
            :disabled="loading"
          >
            <span v-if="loading" class="loading"></span>
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { http } from '../lib/http/client';
import type { BaseResponse } from '../lib/types/base';

const router = useRouter();
const auth = useAuthStore();

const userName = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

const handleLogin = async () => {
  if (!userName.value || !password.value) {
    error.value = '请输入用户名和密码';
    return;
  }
  
  loading.value = true;
  error.value = '';
  
  try {
    const resp = await http.post<BaseResponse<any>>('/api/users/login', {
      userName: userName.value,
      password: password.value,
    });
    const { code, data, msg } = resp.data;
    if (code !== '00000000' || !data) {
      error.value = msg || '登录失败';
      return;
    }
    const name = data.userName || userName.value;
    // 后端暂未下发 token，这里使用占位符，待接入 JWT 后替换
    auth.login('session-token', name);
    await router.replace({ name: 'users' });
  } catch (e: any) {
    error.value = e?.message || '登录失败';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.center-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.w-full {
  width: 100%;
}
</style>


