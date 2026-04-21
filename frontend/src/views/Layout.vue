<template>
  <div class="layout" :class="{ 'is-sidebar-open': sidebarOpen }">
    <!-- Mobile overlay -->
    <div
      v-if="sidebarOpen"
      class="sidebar-overlay"
      role="button"
      aria-label="关闭菜单"
      tabindex="0"
      @click="closeSidebar"
      @keydown.enter="closeSidebar"
      @keydown.space.prevent="closeSidebar"
    />

    <aside class="sidebar" :class="{ open: sidebarOpen }" aria-label="侧边导航">
      <div class="sidebar-top">
        <div class="logo text-2xl font-bold text-primary">故视无极</div>
        <button class="sidebar-close" type="button" aria-label="关闭菜单" @click="closeSidebar">×</button>
      </div>
      <nav class="nav">
        <router-link to="/dashboard" class="nav-link" @click="closeSidebar">仪表盘</router-link>
        <router-link to="/article-generation-configs" class="nav-link" @click="closeSidebar">文章生成配置</router-link>
        <router-link to="/articles" class="nav-link" @click="closeSidebar">文章管理</router-link>
        <router-link to="/dictionaries" class="nav-link" @click="closeSidebar">字典管理</router-link>
        <router-link v-if="showUserManagementNav" to="/users" class="nav-link" @click="closeSidebar">用户管理</router-link>
        <router-link v-if="showUserManagementNav" to="/membership-pricing" class="nav-link" @click="closeSidebar"
          >会员定价</router-link
        >
      </nav>
    </aside>
    
    <main class="main">
      <header class="header">
        <div class="header-left">
          <button class="menu-btn" type="button" @click="toggleSidebar" aria-label="打开菜单">
            <span class="menu-icon" aria-hidden="true"></span>
          </button>
          <h1 class="text-xl font-semibold text-text header-title">管理后台</h1>
        </div>
        <div class="flex items-center gap-4">
          <button class="welcome-btn" type="button" @click="openProfileDialog">
            欢迎，{{ displayName }}
          </button>

          <button
            class="welcome-btn header-wallet-btn"
            type="button"
            @click="openRechargeDialog"
            :title="`可用余额：${formatCentYuan(balance.availableBalanceCent)} 元`"
          >
            余额：{{ formatCentYuan(balance.availableBalanceCent) }} 元
          </button>

          <button
            class="welcome-btn header-membership-btn"
            type="button"
            @click="openMembershipDialog"
            :title="membershipActive ? '会员权益有效' : '开通会员'"
          >
            会员：{{ membershipActive ? '有效' : '未开通' }}
          </button>

          <div class="relative">
            <button
              class="welcome-btn header-notif-btn"
              type="button"
              @click="toggleNotifications"
              :title="`未读消息：${unreadCount}`"
            >
              消息
              <span v-if="unreadCount > 0" class="notif-badge">{{ unreadCount }}</span>
            </button>

            <div v-if="notificationsOpen" class="notif-dropdown">
              <div v-if="notifications.length === 0" class="notif-empty">暂无消息</div>

              <div v-else>
                <div
                  v-for="n in notifications.slice(0, 10)"
                  :key="n.id"
                  class="notif-item"
                >
                  <div class="notif-item-title">
                    {{ n.title || '通知' }}
                  </div>
                  <div class="notif-item-content">
                    {{ n.content || '' }}
                  </div>
                  <div class="notif-item-time">
                    {{ formatDateTime(n.createTime) }}
                  </div>
                  <div class="notif-item-actions">
                    <button
                      class="btn btn-outline btn-xs"
                      type="button"
                      :disabled="n.isRead"
                      @click="markNotificationRead(n.id)"
                    >
                      {{ n.isRead ? '已读' : '标记已读' }}
                    </button>
                  </div>
                </div>

                <div class="notif-footer">
                  <button class="btn btn-outline btn-sm" type="button" @click="markAllNotificationsRead">
                    全部已读
                  </button>
                </div>
              </div>
            </div>
          </div>

          <button @click="logout" class="btn btn-outline btn-sm">退出登录</button>
        </div>
      </header>
      
      <div class="content">
        <router-view />
      </div>

      <footer class="icp-footer">
        <a
          class="icp-link"
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
        >
          沪ICP备2026010028号-1
        </a>
      </footer>
    </main>

    <!-- 用户信息编辑弹窗 -->
    <div v-if="showProfileModal" class="modal-overlay" @click="cancelProfile">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">修改用户信息</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="saveProfile" class="space-y-4">
            <div class="form-group">
              <label class="form-label">用户名</label>
              <input v-model="profileForm.userName" class="form-input" placeholder="请输入用户名" />
            </div>
            <div class="form-group">
              <label class="form-label">笔名</label>
              <input v-model="profileForm.penName" class="form-input" placeholder="请输入笔名" />
            </div>
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <input v-model="profileForm.userEmail" class="form-input" placeholder="请输入邮箱" />
            </div>
            <div class="form-group">
              <label class="form-label">手机号</label>
              <input v-model="profileForm.userPhone" class="form-input" placeholder="请输入手机号" />
            </div>
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input v-model="profileForm.userPassword" type="password" class="form-input" placeholder="不修改请留空" />
            </div>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn btn-outline btn-sm" @click="cancelProfile">取消</button>
              <button type="submit" class="btn btn-primary btn-sm" :disabled="savingProfile">
                {{ savingProfile ? '保存中...' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <div v-if="showRechargeModal" class="modal-overlay" @click="closeRechargeDialog">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">余额充值</h3>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label class="form-label">充值金额（元）</label>
            <input
              v-model.number="rechargeAmountYuan"
              class="form-input"
              type="number"
              min="1"
              step="1"
              placeholder="请输入金额"
              :disabled="creatingRecharge"
            />
          </div>

          <div class="form-group">
            <label class="form-label">支付渠道</label>
            <select v-model="rechargeChannel" class="form-input" :disabled="creatingRecharge">
              <option value="ALIPAY">支付宝</option>
              <option value="WECHAT">微信</option>
            </select>
          </div>

          <div v-if="rechargeOrderNo" class="recharge-order">
            <div class="recharge-row">
              <span class="recharge-label">订单号：</span>
              <span class="recharge-value">{{ rechargeOrderNo }}</span>
            </div>
            <div class="recharge-row">
              <span class="recharge-label">状态：</span>
              <span class="recharge-value">{{ rechargeOrderStatus }}</span>
            </div>

            <div class="form-group" style="margin-top: 1rem;">
              <label class="form-label">支付内容（二维码内容/URL）</label>
              <textarea
                v-model="rechargePayUrl"
                class="form-textarea"
                rows="4"
                readonly
              />
            </div>

            <div class="flex justify-end gap-2">
              <button class="btn btn-outline btn-sm" type="button" @click="copyRechargePayUrl" :disabled="!rechargePayUrl">
                复制支付内容
              </button>
              <a
                v-if="looksLikeUrl(rechargePayUrl)"
                class="btn btn-primary btn-sm"
                :href="rechargePayUrl"
                target="_blank"
                rel="noopener noreferrer"
              >
                打开支付
              </a>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-outline" type="button" @click="closeRechargeDialog" :disabled="creatingRecharge">
            关闭
          </button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="creatingRecharge || rechargeAmountYuan <= 0"
            @click="createRechargeOrder"
          >
            {{ creatingRecharge ? '创建中...' : '创建充值订单' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 开通会员 -->
    <div v-if="showMembershipModal" class="modal-overlay" @click="closeMembershipDialog">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">开通会员</h3>
        </div>
        <div class="card-body">
          <p class="text-sm text-text-secondary mb-3">购买成功后会员时长将自动顺延；仍按余额扣 AI 费用。</p>
          <div class="form-group">
            <label class="form-label">套餐</label>
            <select v-model="selectedPricingId" class="form-input" :disabled="membershipLoading || creatingMembership">
              <option v-for="p in membershipPricing" :key="p.id" :value="p.id">
                {{ p.durationMonths }} 个月 — {{ (p.priceCent / 100).toFixed(2) }} 元
              </option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">支付渠道</label>
            <select v-model="membershipChannel" class="form-input" :disabled="creatingMembership">
              <option value="ALIPAY">支付宝</option>
              <option value="WECHAT">微信</option>
            </select>
          </div>
          <div v-if="membershipOrderNo" class="recharge-order">
            <div class="recharge-row">
              <span class="recharge-label">订单号：</span>
              <span class="recharge-value">{{ membershipOrderNo }}</span>
            </div>
            <div class="recharge-row">
              <span class="recharge-label">状态：</span>
              <span class="recharge-value">{{ membershipOrderStatus }}</span>
            </div>
            <div class="form-group" style="margin-top: 1rem;">
              <label class="form-label">支付内容</label>
              <textarea v-model="membershipPayUrl" class="form-textarea" rows="4" readonly />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" type="button" @click="closeMembershipDialog" :disabled="creatingMembership">
            关闭
          </button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="creatingMembership || !selectedPricingId"
            @click="createMembershipOrder"
          >
            {{ creatingMembership ? '创建中...' : '创建订单' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth';
import { useWalletStore } from '../stores/wallet';
import { http } from '../lib/http/client';
import type { BaseResponse } from '../lib/types/base';

const router = useRouter();
const auth = useAuthStore();
const { isAdmin, membershipActive } = storeToRefs(auth);
const showUserManagementNav = computed(() => isAdmin.value);
const userName = computed(() => auth.userName || '');
const penName = computed(() => auth.penName || '');
const displayName = computed(() => penName.value || userName.value || '用户');

const wallet = useWalletStore();
const { balance, unreadCount, notifications } = storeToRefs(wallet);
const notificationsOpen = ref(false);

const formatCentYuan = (cent: number) => {
  const n = typeof cent === 'number' && Number.isFinite(cent) ? cent : 0;
  return (n / 100).toFixed(2);
};

const formatDateTime = (time: string) => {
  if (!time) return '-';
  const d = new Date(time);
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString('zh-CN');
};

const toggleNotifications = async () => {
  notificationsOpen.value = !notificationsOpen.value;
  if (notificationsOpen.value) {
    await wallet.refreshNotifications();
  }
};

const markNotificationRead = async (id: number) => {
  await wallet.markRead(id);
};

const markAllNotificationsRead = async () => {
  await wallet.markAllRead();
};

const openRechargeDialog = () => {
  rechargeAmountYuan.value = 100;
  rechargeChannel.value = 'ALIPAY';
  rechargeOrderNo.value = '';
  rechargePayUrl.value = '';
  rechargeOrderStatus.value = '';
  showRechargeModal.value = true;
};

const closeRechargeDialog = () => {
  showRechargeModal.value = false;
  stopRechargePolling();
};

// 充值弹窗状态
const showRechargeModal = ref(false);
const creatingRecharge = ref(false);
const rechargeAmountYuan = ref(100);
const rechargeChannel = ref<'ALIPAY' | 'WECHAT'>('ALIPAY');
const rechargeOrderNo = ref('');
const rechargePayUrl = ref('');
const rechargeOrderStatus = ref('');
let rechargePollTimer: ReturnType<typeof setInterval> | null = null;

const stopRechargePolling = () => {
  if (rechargePollTimer) {
    clearInterval(rechargePollTimer);
    rechargePollTimer = null;
  }
};

const looksLikeUrl = (s: string) => {
  if (!s) return false;
  const t = s.trim();
  return t.startsWith('http://') || t.startsWith('https://') || t.startsWith('weixin://');
};

const copyRechargePayUrl = async () => {
  try {
    if (!rechargePayUrl.value) return;
    await navigator.clipboard.writeText(rechargePayUrl.value);
    (window as any)?.showNotification?.('已复制支付内容', 'success');
  } catch {
    (window as any)?.showNotification?.('复制失败，请手动复制', 'error');
  }
};

type RechargeCreateRespDto = { orderNo: string; payUrl: string; expireTime: string };
type RechargeOrderDto = { orderNo: string; status: string; payUrl?: string };

const createRechargeOrder = async () => {
  if (!auth.userId) return;
  if (creatingRecharge.value) return;
  creatingRecharge.value = true;

  stopRechargePolling();

  try {
    const amountCent = Math.round((rechargeAmountYuan.value || 0) * 100);
    const resp = await http.post<BaseResponse<RechargeCreateRespDto>>(
      '/api/wallet/recharge/create',
      { amountCent, channel: rechargeChannel.value },
      { silentBizError: false } as any
    );

    if (resp.data.code !== '00000000' || !resp.data.data) {
      throw new Error(resp.data.msg || '创建充值订单失败');
    }

    rechargeOrderNo.value = resp.data.data.orderNo;
    rechargePayUrl.value = resp.data.data.payUrl;
    rechargeOrderStatus.value = 'PAYING';

    // 轮询订单状态，直到 PAID
    rechargePollTimer = setInterval(async () => {
      if (!rechargeOrderNo.value) return;
      try {
        const r = await http.get<BaseResponse<RechargeOrderDto>>(
          `/api/wallet/recharge/${encodeURIComponent(rechargeOrderNo.value)}`,
          { silentBizError: true } as any
        );
        const order = r.data.data;
        rechargeOrderStatus.value = order?.status || '';

        if (order?.status === 'PAID') {
          stopRechargePolling();
          await wallet.refreshAll();
          (window as any)?.showNotification?.('充值成功', 'success');
          showRechargeModal.value = false;
        }

        if (order?.status === 'FAILED' || order?.status === 'CLOSED') {
          stopRechargePolling();
          await wallet.refreshAll();
          (window as any)?.showNotification?.('充值失败，请稍后重试', 'error');
          showRechargeModal.value = false;
        }
      } catch {
        // ignore polling errors
      }
    }, 2000);
  } catch (e: any) {
    (window as any)?.showNotification?.(e?.message || '创建充值订单失败', 'error');
  } finally {
    creatingRecharge.value = false;
  }
};

type MembershipPricingItem = { id: number; tier: string; durationMonths: number; priceCent: number };

const showMembershipModal = ref(false);
const membershipLoading = ref(false);
const membershipPricing = ref<MembershipPricingItem[]>([]);
const selectedPricingId = ref<number | null>(null);
const membershipChannel = ref<'ALIPAY' | 'WECHAT'>('ALIPAY');
const creatingMembership = ref(false);
const membershipOrderNo = ref('');
const membershipPayUrl = ref('');
const membershipOrderStatus = ref('');
let membershipPollTimer: ReturnType<typeof setInterval> | null = null;

const stopMembershipPolling = () => {
  if (membershipPollTimer) {
    clearInterval(membershipPollTimer);
    membershipPollTimer = null;
  }
};

const openMembershipDialog = async () => {
  membershipOrderNo.value = '';
  membershipPayUrl.value = '';
  membershipOrderStatus.value = '';
  membershipChannel.value = 'ALIPAY';
  showMembershipModal.value = true;
  membershipLoading.value = true;
  stopMembershipPolling();
  try {
    const resp = await http.get<BaseResponse<MembershipPricingItem[]>>('/api/membership/pricing', {
      silentBizError: true,
    } as any);
    if (resp.data.code === '00000000' && resp.data.data?.length) {
      membershipPricing.value = resp.data.data;
      selectedPricingId.value = resp.data.data[0].id;
    } else {
      membershipPricing.value = [];
      selectedPricingId.value = null;
    }
  } finally {
    membershipLoading.value = false;
  }
};

const closeMembershipDialog = () => {
  showMembershipModal.value = false;
  stopMembershipPolling();
};

const createMembershipOrder = async () => {
  if (!auth.userId || !selectedPricingId.value) return;
  if (creatingMembership.value) return;
  creatingMembership.value = true;
  stopMembershipPolling();
  try {
    const resp = await http.post<BaseResponse<RechargeCreateRespDto>>(
      '/api/membership/order/create',
      { pricingConfigId: selectedPricingId.value, channel: membershipChannel.value },
      { silentBizError: false } as any
    );
    if (resp.data.code !== '00000000' || !resp.data.data) {
      throw new Error(resp.data.msg || '创建会员订单失败');
    }
    membershipOrderNo.value = resp.data.data.orderNo;
    membershipPayUrl.value = resp.data.data.payUrl;
    membershipOrderStatus.value = 'PAYING';

    membershipPollTimer = setInterval(async () => {
      if (!membershipOrderNo.value) return;
      try {
        const r = await http.get<BaseResponse<RechargeOrderDto>>(
          `/api/wallet/recharge/${encodeURIComponent(membershipOrderNo.value)}`,
          { silentBizError: true } as any
        );
        const order = r.data.data;
        membershipOrderStatus.value = order?.status || '';
        if (order?.status === 'PAID') {
          stopMembershipPolling();
          await auth.refreshSession();
          await wallet.refreshAll();
          (window as any)?.showNotification?.('会员开通成功', 'success');
          showMembershipModal.value = false;
        }
        if (order?.status === 'FAILED' || order?.status === 'CLOSED') {
          stopMembershipPolling();
          await wallet.refreshAll();
          (window as any)?.showNotification?.('订单已关闭', 'error');
          showMembershipModal.value = false;
        }
      } catch {
        // ignore
      }
    }, 2000);
  } catch (e: any) {
    (window as any)?.showNotification?.(e?.message || '创建会员订单失败', 'error');
  } finally {
    creatingMembership.value = false;
  }
};

// 初次/登录后拉取钱包与通知
const refreshWalletIfAuthed = async () => {
  if (!auth.userId) return;
  await auth.refreshSession().catch(() => {});
  await wallet.refreshAll().catch(() => {});
};

onMounted(() => {
  refreshWalletIfAuthed();
});

watch(
  () => auth.userId,
  () => {
    refreshWalletIfAuthed();
  }
);

const sidebarOpen = ref(false);
const closeSidebar = () => {
  sidebarOpen.value = false;
};
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value;
};

watch(
  () => router.currentRoute.value.fullPath,
  () => {
    closeSidebar();
  }
);

const showProfileModal = ref(false);
const savingProfile = ref(false);
const profileForm = ref({
  userName: '',
  penName: '',
  userEmail: '',
  userPhone: '',
  userPassword: '',
});

const openProfileDialog = async () => {
  const userId = auth.userId;
  if (!userId) return;
  showProfileModal.value = true;
  // 先用本地缓存填充，再拉一次后端最新信息
  profileForm.value.userName = userName.value;
  profileForm.value.penName = penName.value;
  profileForm.value.userPassword = '';
  try {
    const resp = await http.post<BaseResponse<any>>('/api/users/query', { userId });
    const { code, data } = resp.data;
    if (code === '00000000' && data) {
      profileForm.value.userName = data.userName || profileForm.value.userName;
      profileForm.value.penName = data.penName || profileForm.value.penName;
      profileForm.value.userEmail = data.userEmail || '';
      profileForm.value.userPhone = data.userPhone || '';
    }
  } catch {
    // ignore
  }
};

const cancelProfile = () => {
  showProfileModal.value = false;
  savingProfile.value = false;
};

const saveProfile = async () => {
  const userId = auth.userId;
  if (!userId) return;
  if (savingProfile.value) return;
  savingProfile.value = true;
  try {
    const payload: any = { userId };
    if (profileForm.value.userName) payload.userName = profileForm.value.userName;
    if (profileForm.value.penName !== undefined) payload.penName = profileForm.value.penName;
    if (profileForm.value.userEmail) payload.userEmail = profileForm.value.userEmail;
    if (profileForm.value.userPhone) payload.userPhone = profileForm.value.userPhone;
    if (profileForm.value.userPassword) payload.userPassword = profileForm.value.userPassword;

    const resp = await http.post<BaseResponse<boolean>>('/api/users/update', payload);
    if (resp.data.code === '00000000') {
      auth.setProfile({ userName: profileForm.value.userName, penName: profileForm.value.penName });
      (window as any).showNotification?.('用户信息已更新', 'success');
      showProfileModal.value = false;
    } else {
      (window as any).showNotification?.(resp.data.msg || '更新失败', 'error');
    }
  } catch (e: any) {
    (window as any).showNotification?.(e?.message || '更新失败', 'error');
  } finally {
    savingProfile.value = false;
  }
};

const logout = async () => {
  await auth.logout();
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

.sidebar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.sidebar-close {
  display: none;
  width: 40px;
  height: 40px;
  border-radius: 0.5rem;
  border: 1px solid var(--border);
  background: white;
  cursor: pointer;
  font-size: 1.25rem;
  line-height: 1;
  color: var(--text-secondary);
}

.sidebar-close:hover {
  background: var(--surface);
  color: var(--text);
}

.sidebar-overlay {
  display: none;
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
  display: flex;
  flex-direction: column;
  min-height: 100vh;
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

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.header-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-btn {
  display: none;
  width: 40px;
  height: 40px;
  border-radius: 0.5rem;
  border: 1px solid var(--border);
  background: white;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-btn:hover {
  background: var(--surface);
}

.menu-icon {
  width: 18px;
  height: 2px;
  background: var(--text);
  position: relative;
  display: inline-block;
  border-radius: 9999px;
}

.menu-icon::before,
.menu-icon::after {
  content: '';
  position: absolute;
  left: 0;
  width: 18px;
  height: 2px;
  background: var(--text);
  border-radius: 9999px;
}

.menu-icon::before {
  top: -6px;
}

.menu-icon::after {
  top: 6px;
}

.content {
  padding: 1.5rem;
  flex: 1;
}

.welcome-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 0.375rem;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.welcome-btn:hover {
  background: var(--surface-hover);
  color: var(--primary);
}

.header-wallet-btn {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid var(--border);
  color: var(--text);
}

.header-notif-btn {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid var(--border);
  color: var(--text);
  position: relative;
}

.notif-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 0.5rem;
  min-width: 1.2rem;
  height: 1.2rem;
  padding: 0 0.35rem;
  border-radius: 9999px;
  background: var(--warning-color, #faad14);
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
}

.notif-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 0.5rem);
  width: 340px;
  max-height: 420px;
  overflow-y: auto;
  background: white;
  border: 1px solid var(--border);
  border-radius: 0.75rem;
  box-shadow: var(--shadow);
  padding: 0.75rem;
  z-index: 2000;
}

.notif-empty {
  color: var(--text-secondary);
  padding: 1rem 0.25rem;
}

.notif-item {
  padding: 0.75rem;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 0.5rem;
  margin-bottom: 0.75rem;
}

.notif-item:last-child {
  margin-bottom: 0;
}

.notif-item-title {
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.notif-item-content {
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.875rem;
}

.notif-item-time {
  margin-top: 0.5rem;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.notif-item-actions {
  margin-top: 0.5rem;
  display: flex;
  justify-content: flex-end;
}

.notif-footer {
  margin-top: 0.5rem;
  display: flex;
  justify-content: flex-end;
}

.recharge-order {
  border: 1px dashed var(--border);
  border-radius: 0.75rem;
  padding: 0.75rem;
  margin-top: 1rem;
}

.recharge-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.recharge-label {
  color: var(--text-secondary);
  white-space: nowrap;
}

.recharge-value {
  font-weight: 600;
  word-break: break-all;
}

.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  background: white;
  resize: vertical;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.form-textarea:disabled {
  background: var(--surface);
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.5rem;
  border-top: 1px solid var(--border-light, #e5e7eb);
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.icp-footer {
  padding: 0.75rem 1.5rem;
  text-align: center;
  border-top: 1px solid var(--border);
  background: white;
}

.icp-link {
  font-size: 0.875rem;
  color: var(--text-muted);
  text-decoration: none;
}

.icp-link:hover {
  color: var(--primary);
  text-decoration: underline;
}

@media (max-width: 768px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .menu-btn {
    display: inline-flex;
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.35);
    z-index: 40;
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: auto;
    width: 100%;
    max-height: 55vh;
    border-right: none;
    border-bottom: 1px solid var(--border);
    z-index: 50;
    transform: translateY(-110%);
    transition: transform 0.2s ease;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    padding: 0.75rem 0.75rem;
  }

  .sidebar.open {
    transform: translateY(0);
  }

  .sidebar-close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .logo {
    margin-bottom: 0;
    text-align: left;
    font-size: 1.125rem;
  }

  .nav-link {
    padding: 0.65rem 0.75rem;
  }

  .header {
    padding: 0.75rem 1rem;
  }

  .content {
    padding: 1rem;
  }

  .icp-footer {
    padding: 0.75rem 1rem;
  }
}
</style>


