import { defineStore } from 'pinia';

interface AuthState {
  token: string | null;
  userName: string | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ token: localStorage.getItem('token'), userName: null }),
  getters: {
    isAuthenticated: (s) => !!s.token,
  },
  actions: {
    login(token: string, userName: string) {
      this.token = token;
      this.userName = userName;
      localStorage.setItem('token', token);
    },
    logout() {
      this.token = null;
      this.userName = null;
      localStorage.removeItem('token');
    },
  },
});


