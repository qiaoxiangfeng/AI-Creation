import { defineStore } from 'pinia';
import { http } from '../lib/http/client';
import { parseAdminFlag } from '../lib/parseAdminFlag';
import type { BaseResponse } from '../lib/types/base';

interface AuthState {
  userId: number | null;
  userName: string | null;
  penName: string | null;
  isAdmin: boolean;
  /** 与后端 /users/me 一致；管理员视为有效 */
  membershipActive: boolean;
  authenticated: boolean;
  initialized: boolean;
}

function parseStoredUserId(): number | null {
  const raw = localStorage.getItem('userId');
  if (!raw || !/^\d+$/.test(raw.trim())) return null;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    userId: parseStoredUserId(),
    userName: localStorage.getItem('userName'),
    penName: localStorage.getItem('penName'),
    isAdmin:
      localStorage.getItem('isAdmin') === '1' ||
      localStorage.getItem('isAdmin') === 'true',
    membershipActive: false,
    authenticated: false,
    initialized: false,
  }),
  getters: {
    isAuthenticated: (s) => s.authenticated,
  },
  actions: {
    async refreshSession() {
      try {
        const resp = await http.get<BaseResponse<Record<string, unknown>>>('/api/users/me', {
          silentBizError: true,
        } as any);
        const { code, data } = resp.data;
        if (code !== '00000000' || !data) {
          this.authenticated = false;
          return;
        }
        const id = Number((data as any).id ?? 0);
        if (!Number.isFinite(id) || id <= 0) {
          this.authenticated = false;
          return;
        }
        this.userId = id;
        localStorage.setItem('userId', String(id));
        const isAdmin = parseAdminFlag(data as Record<string, unknown>);
        this.isAdmin = isAdmin;
        localStorage.setItem('isAdmin', isAdmin ? '1' : '0');
        if (typeof data.userName === 'string' && data.userName) {
          this.userName = data.userName;
          localStorage.setItem('userName', data.userName);
        }
        if (data.penName !== undefined) {
          const p = data.penName == null ? '' : String(data.penName);
          this.penName = p || null;
          localStorage.setItem('penName', p);
        }
        this.membershipActive = Boolean((data as any).membershipActive);
        this.authenticated = true;
      } catch {
        this.authenticated = false;
      } finally {
        this.initialized = true;
      }
    },
    async initializeAuth() {
      if (this.initialized) return;
      await this.refreshSession();
    },
    login(user: {
      userId: number;
      userName: string;
      penName?: string | null;
      isAdmin?: boolean | null;
      membershipActive?: boolean | null;
    }) {
      this.userId = user.userId;
      this.userName = user.userName;
      this.penName = user.penName ?? null;
      this.isAdmin = Boolean(user.isAdmin);
      this.membershipActive = Boolean(user.membershipActive);
      localStorage.setItem('userId', String(user.userId));
      localStorage.setItem('userName', user.userName);
      localStorage.setItem('penName', user.penName ?? '');
      localStorage.setItem('isAdmin', this.isAdmin ? '1' : '0');
      this.authenticated = true;
      this.initialized = true;
    },
    setProfile(patch: { userName?: string | null; penName?: string | null }) {
      if (patch.userName !== undefined) {
        this.userName = patch.userName;
        if (patch.userName) localStorage.setItem('userName', patch.userName);
      }
      if (patch.penName !== undefined) {
        this.penName = patch.penName;
        localStorage.setItem('penName', patch.penName ?? '');
      }
    },
    async logout() {
      try {
        await http.post('/api/users/logout', {}, { silentBizError: true } as any);
      } catch {
        // ignore
      }
      this.userId = null;
      this.userName = null;
      this.penName = null;
      this.isAdmin = false;
      this.membershipActive = false;
      localStorage.removeItem('userId');
      localStorage.removeItem('userName');
      localStorage.removeItem('penName');
      localStorage.removeItem('isAdmin');
      this.authenticated = false;
      this.initialized = true;
    },
  },
});


