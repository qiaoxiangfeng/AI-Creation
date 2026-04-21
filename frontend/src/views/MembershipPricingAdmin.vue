<template>
  <div class="card">
    <div class="card-header">
      <h2 class="text-xl font-semibold text-text">会员定价配置</h2>
    </div>
    <div class="card-body">
      <p class="text-sm text-text-secondary mb-4">修改后保存即可；成交价 = 月价 × 月数 × 折扣（四舍五入到分）。</p>
      <div v-if="loading" class="loading-text">加载中...</div>
      <table v-else class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>档位</th>
            <th>月数</th>
            <th>月价(分)</th>
            <th>折扣</th>
            <th>上架</th>
            <th>排序</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.tier }}</td>
            <td>{{ row.durationMonths }}</td>
            <td>
              <input v-model.number="row.baseMonthPriceCent" type="number" class="form-input form-input-sm" min="1" />
            </td>
            <td>
              <input v-model.number="row.discountRate" type="number" step="0.0001" min="0" max="1" class="form-input form-input-sm" />
            </td>
            <td>
              <input v-model="row.enabled" type="checkbox" />
            </td>
            <td>
              <input v-model.number="row.sortOrder" type="number" class="form-input form-input-sm" />
            </td>
            <td>
              <button type="button" class="btn btn-primary btn-sm" :disabled="saving" @click="saveRow(row)">
                保存
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="mt-8 pt-6 border-t border-border">
        <h3 class="text-lg font-semibold mb-2">管理员赠送/顺延（按月）</h3>
        <div class="flex flex-wrap gap-3 items-end">
          <div class="form-group mb-0 min-w-[320px] relative">
            <label class="form-label">选择用户</label>
            <input
              v-model="userComboText"
              type="text"
              class="form-input"
              placeholder="输入用户名/笔名搜索并选择..."
              :disabled="grantSaving"
              @focus="openUserDropdown"
              @input="onUserComboInput"
              @keydown.down.prevent="moveUserCursor(1)"
              @keydown.up.prevent="moveUserCursor(-1)"
              @keydown.enter.prevent="selectUserByCursor"
              @keydown.esc.prevent="closeUserDropdown"
            />
            <div v-if="userDropdownOpen" class="user-dropdown" role="listbox">
              <div v-if="usersLoading" class="user-dropdown-item user-dropdown-hint">搜索中...</div>
              <div v-else-if="users.length === 0" class="user-dropdown-item user-dropdown-hint">无匹配用户</div>
              <button
                v-else
                v-for="(u, idx) in users"
                :key="u.id"
                type="button"
                class="user-dropdown-item"
                :class="{ active: idx === userCursor }"
                @click="selectUser(u)"
              >
                {{ formatUserLabel(u) }}
              </button>
            </div>
          </div>
          <div class="form-group mb-0">
            <label class="form-label">月数</label>
            <input v-model.number="grantMonths" type="number" min="1" class="form-input" />
          </div>
          <div class="form-group mb-0 flex-1 min-w-[200px]">
            <label class="form-label">备注</label>
            <input v-model="grantRemark" type="text" class="form-input" placeholder="可选" />
          </div>
          <button type="button" class="btn btn-primary" :disabled="grantSaving" @click="submitGrant">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { http } from '../lib/http/client';
import type { BaseResponse, PageRespDto } from '../lib/types/base';

type Row = {
  id: number;
  tier: string;
  durationMonths: number;
  baseMonthPriceCent: number;
  discountRate: number;
  enabled: boolean;
  sortOrder: number;
};

const rows = ref<Row[]>([]);
const loading = ref(true);
const saving = ref(false);
const grantUserId = ref<number | null>(null);
const grantMonths = ref(1);
const grantRemark = ref('');
const grantSaving = ref(false);
const usersLoading = ref(false);
const users = ref<{ id: number; userName: string; penName?: string | null }[]>([]);
const userDropdownOpen = ref(false);
const userComboText = ref('');
const userCursor = ref(-1);
let userSearchTimer: ReturnType<typeof setTimeout> | null = null;

const load = async () => {
  loading.value = true;
  try {
    const resp = await http.get<BaseResponse<Row[]>>('/api/admin/membership/pricing/list');
    if (resp.data.code === '00000000' && resp.data.data) {
      rows.value = (resp.data.data || []).map((r: any) => ({
        ...r,
        discountRate: Number(r.discountRate),
      }));
    }
  } finally {
    loading.value = false;
  }
};

const saveRow = async (row: Row) => {
  saving.value = true;
  try {
    await http.post<BaseResponse<boolean>>('/api/admin/membership/pricing/save', {
      id: row.id,
      tier: row.tier,
      durationMonths: row.durationMonths,
      baseMonthPriceCent: row.baseMonthPriceCent,
      discountRate: row.discountRate,
      enabled: row.enabled,
      sortOrder: row.sortOrder,
    });
    (window as any)?.showNotification?.('已保存', 'success');
  } catch (e: any) {
    (window as any)?.showNotification?.(e?.message || '保存失败', 'error');
  } finally {
    saving.value = false;
  }
};

const loadUsers = async () => {
  usersLoading.value = true;
  try {
    const kw = userComboText.value.trim();
    const resp = await http.post<BaseResponse<PageRespDto<any>>>('/api/users/list', {
      pageNo: 1,
      pageSize: kw ? 50 : 3,
      userName: kw || undefined,
    });
    const page = resp.data.data;
    users.value = (page?.list || []).map((u: any) => ({
      id: Number(u.id),
      userName: String(u.userName || ''),
      penName: u.penName ?? null,
    })).filter((u: any) => u.id > 0 && u.userName);
    if (users.value.length > 0) {
      userCursor.value = 0;
    } else {
      userCursor.value = -1;
    }
  } catch {
    // ignore
  } finally {
    usersLoading.value = false;
  }
};

const formatUserLabel = (u: { id: number; userName: string; penName?: string | null }) => {
  return `${u.userName}${u.penName ? `（${u.penName}）` : ''} #${u.id}`;
};

const openUserDropdown = () => {
  userDropdownOpen.value = true;
  // 初次打开时，若没有数据则触发一次搜索
  if (users.value.length === 0 && !usersLoading.value) {
    loadUsers();
  }
};

const closeUserDropdown = () => {
  userDropdownOpen.value = false;
};

const selectUser = (u: { id: number; userName: string; penName?: string | null }) => {
  grantUserId.value = u.id;
  userComboText.value = formatUserLabel(u);
  closeUserDropdown();
};

const moveUserCursor = (delta: number) => {
  if (!userDropdownOpen.value) {
    openUserDropdown();
  }
  if (!users.value.length) return;
  const next = userCursor.value + delta;
  if (next < 0) userCursor.value = users.value.length - 1;
  else if (next >= users.value.length) userCursor.value = 0;
  else userCursor.value = next;
};

const selectUserByCursor = () => {
  if (!userDropdownOpen.value) {
    openUserDropdown();
    return;
  }
  const idx = userCursor.value;
  if (idx < 0 || idx >= users.value.length) return;
  selectUser(users.value[idx]);
};

const onUserComboInput = () => {
  // 输入变化时认为未选择（避免显示 label 但 id 不匹配）
  grantUserId.value = null;
  openUserDropdown();
  if (userSearchTimer) clearTimeout(userSearchTimer);
  userSearchTimer = setTimeout(() => {
    loadUsers();
  }, 250);
};

const submitGrant = async () => {
  if (!grantUserId.value || grantUserId.value <= 0 || !grantMonths.value || grantMonths.value < 1) {
    (window as any)?.showNotification?.('请选择用户并填写有效月数', 'error');
    return;
  }
  grantSaving.value = true;
  try {
    await http.post<BaseResponse<boolean>>('/api/admin/membership/grant-months', {
      targetUserId: grantUserId.value,
      months: grantMonths.value,
      remark: grantRemark.value || undefined,
    });
    (window as any)?.showNotification?.('已顺延会员', 'success');
  } catch (e: any) {
    (window as any)?.showNotification?.(e?.message || '操作失败', 'error');
  } finally {
    grantSaving.value = false;
  }
};

onMounted(() => {
  load();
  loadUsers();
});
</script>

<style scoped>
.card {
  /* 全局 .card 默认 overflow: hidden，会裁剪下拉框 */
  overflow: visible;
}

.form-input-sm {
  max-width: 120px;
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 0.25rem);
  left: 0;
  right: 0;
  z-index: 5000;
  background: #fff;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 0.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  max-height: 260px;
  overflow: auto;
}

.user-dropdown-item {
  width: 100%;
  text-align: left;
  padding: 0.5rem 0.75rem;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.user-dropdown-item:hover,
.user-dropdown-item.active {
  background: #f3f4f6;
}

.user-dropdown-hint {
  color: #6b7280;
  cursor: default;
}
</style>
