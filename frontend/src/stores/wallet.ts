import { defineStore } from 'pinia';
import { http } from '../lib/http/client';
import type { BaseResponse } from '../lib/types/base';

export interface WalletBalanceRespDto {
  totalBalanceCent: number;
  frozenBalanceCent: number;
  availableBalanceCent: number;
}

export interface UserNotification {
  id: number;
  userId: number;
  type: string;
  title: string;
  content: string;
  isRead: boolean;
  readTime?: string;
  bizRefType?: string;
  bizRefId?: number;
  createTime: string;
}

export const useWalletStore = defineStore('wallet', {
  state: () => ({
    balance: {
      totalBalanceCent: 0,
      frozenBalanceCent: 0,
      availableBalanceCent: 0,
    } as WalletBalanceRespDto,
    unreadCount: 0,
    notifications: [] as UserNotification[],
  }),
  actions: {
    async refreshBalance() {
      const resp = await http.get<BaseResponse<WalletBalanceRespDto>>('/api/wallet/balance', {
        silentBizError: true,
      } as any);
      if (resp.data?.code === '00000000' && resp.data.data) {
        this.balance = resp.data.data;
      }
    },
    async refreshUnreadCount() {
      const resp = await http.get<BaseResponse<number>>('/api/notifications/unread-count', {
        silentBizError: true,
      } as any);
      if (resp.data?.code === '00000000' && typeof resp.data.data === 'number') {
        this.unreadCount = resp.data.data;
      }
    },
    async refreshNotifications() {
      const resp = await http.post<BaseResponse<UserNotification[]>>(
        '/api/notifications/list',
        {},
        { silentBizError: true } as any
      );
      if (resp.data?.code === '00000000') {
        this.notifications = (resp.data.data || []) as UserNotification[];
      }
    },
    async refreshAll() {
      await Promise.all([this.refreshBalance(), this.refreshUnreadCount(), this.refreshNotifications()]);
    },
    async markRead(id: number) {
      await http.post<BaseResponse<boolean>>(
        '/api/notifications/read',
        { id },
        { silentBizError: true } as any
      );
      await this.refreshAll();
    },
    async markAllRead() {
      await http.post<BaseResponse<boolean>>('/api/notifications/read-all', {}, { silentBizError: true } as any);
      await this.refreshAll();
    },
  },
});

