<template>
  <div>
    <div class="card mb-6">
      <div class="card-header">
        <h2 class="text-xl font-semibold text-text">用户管理</h2>
      </div>
      
      <div class="card-body">
        <div class="toolbar">
          <div class="search-section">
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="请输入用户名搜索..."
              @keyup.enter="handleSearch"
            />
            <button @click="handleSearch" class="btn btn-primary">
              搜索
            </button>
            <button @click="clearSearch" class="btn btn-outline">
              清空
            </button>
          </div>
          
          <div class="action-buttons">
            <button @click="showCreateModal = true" class="btn btn-primary">
              新增用户
            </button>
            <button @click="showPasswordModal = true" class="btn btn-secondary ml-4">
              批量重置密码
            </button>
          </div>
        </div>
        
        <div class="table-container">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">加载中...</p>
          </div>
          
          <table v-else-if="users.length > 0" class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>笔名</th>
                <th>邮箱</th>
                <th>手机号</th>
                <th>状态</th>
                <th>管理员</th>
                <th>创建时间</th>
                <th class="action-column">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.userName }}</td>
                <td>{{ user.penName || '-' }}</td>
                <td>{{ user.userEmail || '-' }}</td>
                <td>{{ user.userPhone || '-' }}</td>
                <td>
                  <span :class="getStatusBadgeClass(user.userStatus)">
                    {{ getStatusText(user.userStatus) }}
                  </span>
                </td>
                <td>{{ user.isAdmin ? '是' : '否' }}</td>
                <td>{{ formatDate(user.createTime) }}</td>
                <td class="action-column">
                  <div class="flex gap-2">
                    <button @click="editUser(user)" class="btn btn-outline btn-sm">
                      编辑
                    </button>
                    <select @change="handleActionSelect($event, user)" class="action-select">
                      <option value="">更多操作</option>
                      <option :value="'initPassword_' + user.id">
                        初始化密码
                      </option>
                      <option :value="'addBalance_' + user.id">
                        补入余额
                      </option>
                      <option
                        :value="'delete_' + user.id"
                        class="text-error"
                      >
                        删除
                      </option>
                    </select>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          
          <div v-else class="empty-state">
            <p class="empty-text">暂无用户数据</p>
          </div>
        </div>
        
        <div v-if="total > 0" class="pagination">
          <div class="pagination-info">
            <span class="text-sm text-text-secondary">
              共 {{ total }} 条记录，第 {{ currentPage }} 页，共 {{ totalPages }} 页
            </span>
          </div>
          
          <div class="pagination-controls">
            <button
              @click="changePage(currentPage - 1)"
              :disabled="currentPage <= 1"
              class="pagination-item"
            >
              上一页
            </button>
            <span class="pagination-item">
              {{ currentPage }} / {{ totalPages }}
            </span>
            <button
              @click="changePage(currentPage + 1)"
              :disabled="currentPage >= totalPages"
              class="pagination-item"
            >
              下一页
            </button>
          </div>
          
          <div class="pagination-size">
            <label class="text-sm text-text-secondary">每页显示：</label>
            <select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 创建用户模态框 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="showCreateModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">新增用户</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="createUser" class="space-y-4">
            <div class="form-group">
              <label class="form-label">用户名</label>
              <input v-model="newUser.userName" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">笔名</label>
              <input v-model="newUser.penName" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <input v-model="newUser.userEmail" type="email" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">手机号</label>
              <input v-model="newUser.userPhone" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">密码</label>
              <input v-model="newUser.userPassword" type="password" class="form-input" required />
            </div>
            <div class="form-group flex items-center gap-2">
              <input id="newUserAdmin" v-model="newUser.isAdmin" type="checkbox" class="form-checkbox" />
              <label for="newUserAdmin" class="form-label mb-0">管理员</label>
            </div>
            <div class="flex gap-2 justify-end">
              <button type="button" @click="showCreateModal = false" class="btn btn-outline">
                取消
              </button>
              <button type="submit" class="btn btn-primary">确定</button>
            </div>
          </form>
        </div>
      </div>
    </div>
    
    <!-- 编辑用户模态框 -->
    <div v-if="showEditModal" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">编辑用户</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="updateUser" class="space-y-4">
            <div class="form-group">
              <label class="form-label">用户名</label>
              <input v-model="editingUser!.userName" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">笔名</label>
              <input v-model="editingUser!.penName" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <input v-model="editingUser!.userEmail" type="email" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">手机号</label>
              <input v-model="editingUser!.userPhone" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input v-model="editingUser!.userPassword" type="password" class="form-input" placeholder="留空则不修改密码" />
            </div>
            <div class="form-group flex items-center gap-2">
              <input id="editUserAdmin" v-model="editingUser!.isAdmin" type="checkbox" class="form-checkbox" />
              <label for="editUserAdmin" class="form-label mb-0">管理员</label>
            </div>
            <div class="flex gap-2 justify-end">
              <button type="button" @click="showEditModal = false" class="btn btn-outline">
                取消
              </button>
              <button type="submit" class="btn btn-primary">确定</button>
            </div>
          </form>
        </div>
      </div>
    </div>
    
    <!-- 批量重置密码模态框 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click="showPasswordModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">批量重置密码</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="resetPasswords" class="space-y-4">
            <div class="form-group">
              <label class="form-label">新密码</label>
              <input v-model="newPassword" type="password" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">确认密码</label>
              <input v-model="confirmPassword" type="password" class="form-input" required />
            </div>
            <div class="flex gap-2 justify-end">
              <button type="button" @click="showPasswordModal = false" class="btn btn-outline">
                取消
              </button>
              <button type="submit" class="btn btn-primary">确定</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 补入余额模态框 -->
    <div v-if="showAddBalanceModal" class="modal-overlay" @click="showAddBalanceModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">补入余额</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="submitAddBalance" class="space-y-4">
            <div class="form-group">
              <label class="form-label">补入金额（元）</label>
              <input
                v-model.number="addBalanceAmountYuan"
                type="number"
                min="1"
                step="1"
                class="form-input"
                placeholder="例如：100"
                required
                :disabled="creatingAddBalance"
              />
            </div>

            <div class="form-group">
              <label class="form-label">备注</label>
              <input
                v-model="addBalanceRemark"
                class="form-input"
                placeholder="例如：线下转账补记"
                :disabled="creatingAddBalance"
              />
            </div>

            <div class="flex gap-2 justify-end">
              <button type="button" @click="showAddBalanceModal = false" class="btn btn-outline" :disabled="creatingAddBalance">
                取消
              </button>
              <button type="submit" class="btn btn-primary" :disabled="creatingAddBalance">
                {{ creatingAddBalance ? '提交中...' : '提交补入' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { http } from '../lib/http/client';
import type { BaseResponse, PageRespDto } from '../lib/types/base';

interface UserRespDto {
  id: number;
  userName: string;
  penName?: string;
  userEmail?: string;
  userPhone?: string;
  userStatus: number;
  isAdmin?: boolean;
  createTime: string;
}

const users = ref<UserRespDto[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const searchKeyword = ref('');

const showCreateModal = ref(false);
const showEditModal = ref(false);
const showPasswordModal = ref(false);
const showAddBalanceModal = ref(false);

const addBalanceTargetUserId = ref<number | null>(null);
const addBalanceAmountYuan = ref<number>(100);
const addBalanceRemark = ref<string>('线下转账补记');
const creatingAddBalance = ref(false);


const newUser = ref({ 
  userName: '', 
  penName: '',
  userEmail: '', 
  userPhone: '', 
  userPassword: '',
  isAdmin: false,
});
const editingUser = ref<{ 
  id: number; 
  userName: string; 
  penName?: string;
  userEmail?: string; 
  userPhone?: string; 
  userStatus: number; 
  isAdmin?: boolean;
  createTime: string; 
  userPassword?: string; 
} | null>(null);
const newPassword = ref('');
const confirmPassword = ref('');

const totalPages = computed(() => Math.ceil(total.value / pageSize.value));

onMounted(() => {
  loadUsers();
});

const loadUsers = async () => {
  loading.value = true;
  try {
    const resp = await http.post<BaseResponse<PageRespDto<UserRespDto>>>('/api/users/list', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      userName: searchKeyword.value.trim() || undefined,
    });
    const page = resp.data.data;
    users.value = page?.list || [];
    total.value = page?.total || 0;
    // 使用后端返回的分页信息更新当前页
    currentPage.value = page?.pageNo || 1;
  } catch (error: any) {
    console.error('加载用户列表失败:', error);
    const errorMessage = error.message || '加载用户列表失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1; // 搜索时重置到第一页
  loadUsers();
};

// 下拉列表操作
const handleActionSelect = async (event: Event, user: UserRespDto) => {
  const target = event.target as HTMLSelectElement;
  const value = target.value;

  if (!value) return; // 如果选择的是"更多操作"，不做任何操作

  const [action, userIdStr] = value.split('_');
  const userId = parseInt(userIdStr);

  if (action === 'initPassword') {
    initPassword(user);
  } else if (action === 'delete') {
    deleteUser(user);
  } else if (action === 'addBalance') {
    addBalanceTargetUserId.value = user.id;
    addBalanceAmountYuan.value = 100;
    addBalanceRemark.value = '线下转账补记';
    showAddBalanceModal.value = true;
  }

  // 重置选择
  target.value = '';
};

const clearSearch = () => {
  searchKeyword.value = '';
  currentPage.value = 1;
  loadUsers();
};

const changePage = (page: number) => {
  currentPage.value = page;
  loadUsers();
};

const handlePageSizeChange = () => {
  currentPage.value = 1; // 切换每页大小时重置到第一页
  loadUsers();
};

const createUser = async () => {
  try {
    const payload: any = {
      userName: newUser.value.userName?.trim(),
      userPassword: newUser.value.userPassword
    };
    if (newUser.value.penName && newUser.value.penName.trim()) {
      payload.penName = newUser.value.penName.trim();
    }
    if (newUser.value.userEmail && newUser.value.userEmail.trim()) {
      payload.userEmail = newUser.value.userEmail.trim();
    }
    if (newUser.value.userPhone && newUser.value.userPhone.trim()) {
      payload.userPhone = newUser.value.userPhone.trim();
    }
    if (newUser.value.isAdmin) {
      payload.isAdmin = true;
    }

    await http.post('/api/users', payload);
    showCreateModal.value = false;
    newUser.value = { userName: '', penName: '', userEmail: '', userPhone: '', userPassword: '', isAdmin: false };
    loadUsers();
  } catch (error) {
    console.error('创建用户失败:', error);
    const errorMessage = (error as any)?.message || '创建用户失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

const editUser = (user: UserRespDto) => {
  editingUser.value = { ...user, isAdmin: Boolean(user.isAdmin), userPassword: '' };
  showEditModal.value = true;
};

const updateUser = async () => {
  if (!editingUser.value) return;
  
  try {
    const updateData: any = { 
      userId: editingUser.value.id, 
      userName: editingUser.value.userName,
      penName: editingUser.value.penName,
      isAdmin: Boolean(editingUser.value.isAdmin),
      userEmail: editingUser.value.userEmail,
      userPhone: editingUser.value.userPhone
    };
    
    // 只有当密码字段有值时才传递
    if (editingUser.value.userPassword) {
      updateData.userPassword = editingUser.value.userPassword;
    }
    
    await http.post('/api/users/update', updateData);
    showEditModal.value = false;
    editingUser.value = null;
    loadUsers();
  } catch (error) {
    console.error('更新用户失败:', error);
    const errorMessage = (error as any)?.message || '更新用户失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

const deleteUser = async (user: UserRespDto) => {
  
  try {
    await http.post('/api/users/delete', { userId: user.id });
    loadUsers();
  } catch (error) {
    console.error('删除用户失败:', error);
    const errorMessage = (error as any)?.message || '删除用户失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

const initPassword = async (user: UserRespDto) => {
  if (!confirm(`确定要初始化用户 "${user.userName}" 的密码吗？`)) return;
  
  try {
    await http.post('/api/users/init-password', { 
      userIds: [user.id],
      newPassword: '123456'
    });
    window.showNotification('密码初始化成功！', 'success');
  } catch (error: any) {
    console.error('初始化密码失败:', error);
    const errorMessage = error.message || '初始化密码失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

const submitAddBalance = async () => {
  if (!addBalanceTargetUserId.value) return;
  if (creatingAddBalance.value) return;

  const amountYuan = addBalanceAmountYuan.value;
  if (!Number.isFinite(amountYuan) || amountYuan <= 0) {
    window.showNotification('请输入正确的金额（大于0）', 'error');
    return;
  }

  creatingAddBalance.value = true;
  try {
    const amountCent = Math.round(amountYuan * 100);
    const idempotencyKey =
      'admin-add-balance:' +
      addBalanceTargetUserId.value +
      ':' +
      Date.now() +
      ':' +
      Math.random().toString(16).slice(2);

    await http.post(
      `/api/users/${addBalanceTargetUserId.value}/wallet/add-balance`,
      {
        amountCent,
        remark: addBalanceRemark.value,
        idempotencyKey,
      },
      { silentBizError: false } as any
    );

    showAddBalanceModal.value = false;
    addBalanceTargetUserId.value = null;
    addBalanceAmountYuan.value = 100;
    addBalanceRemark.value = '线下转账补记';
    window.showNotification('补入余额成功', 'success');
    // 不强制刷新用户列表（余额不在表里），但保持一致性
    loadUsers();
  } catch (e: any) {
    window.showNotification(e?.message || '补入余额失败，请稍后重试', 'error');
  } finally {
    creatingAddBalance.value = false;
  }
};

const resetPasswords = async () => {
  if (newPassword.value !== confirmPassword.value) {
    window.showNotification('两次输入的密码不一致', 'warning');
    return;
  }
  
  try {
    await http.post('/api/users/init-password', { 
      userIds: [], // 空数组表示所有用户
      newPassword: newPassword.value 
    });
    showPasswordModal.value = false;
    newPassword.value = '';
    confirmPassword.value = '';
    window.showNotification('密码重置成功', 'success');
  } catch (error: any) {
    console.error('重置密码失败:', error);
    const errorMessage = error.message || '重置密码失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-CN');
};

const getStatusText = (status: number) => {
  return status === 1 ? '正常' : '禁用';
};

const getStatusBadgeClass = (status: number) => {
  return status === 1 ? 'badge badge-success' : 'badge badge-warning';
};
</script>

<style scoped>
.card {
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border);
}

.card-body {
  padding: 1.5rem;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.search-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.search-input {
  flex: 1;
  max-width: 300px;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  background: white;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}



.action-column {
  width: 120px;
}

.table-container {
  overflow-x: auto;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: grid;
  place-items: center;
  z-index: 50;
}

.modal-content {
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text);
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 2px solid var(--border);
  border-top: 2px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

.loading-text {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.empty-text {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  background-color: var(--surface);
  border-radius: 0.5rem;
  border: 1px solid var(--border);
}

.pagination-info {
  flex: 1;
  text-align: left;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-item {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-item:hover:not(:disabled) {
  background-color: var(--primary-light);
  border-color: var(--primary);
}

.pagination-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: var(--text-secondary);
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-size-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-size-select:hover {
  border-color: var(--primary);
}

.badge {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-success {
  background-color: var(--success-light);
  color: var(--success);
  border: 1px solid var(--success-light);
}

.badge-warning {
  background-color: var(--warning-light);
  color: var(--warning);
  border: 1px solid var(--warning-light);
}

/* 下拉菜单样式 */
.dropdown {
  position: relative;
}

.dropdown.open .dropdown-menu {
  display: block;
}

.dropdown-toggle {
  cursor: pointer;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 10;
  display: none;
  min-width: 120px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-top: 0.25rem;
}

.dropdown-menu-item {
  display: block;
  width: 100%;
  padding: 0.5rem 1rem;
  text-align: left;
  border: none;
  background: none;
  color: var(--text);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dropdown-menu-item:hover {
  background-color: var(--surface);
}

.dropdown-menu-item.text-error {
  color: var(--error);
}

.dropdown-menu-item.text-error:hover {
  background-color: var(--error-light);
  color: var(--error);
}

.flex {
  display: flex;
}

.gap-2 {
  gap: 0.5rem;
}

.justify-end {
  justify-content: flex-end;
}

.text-sm {
  font-size: 0.875rem;
}

.text-lg {
  font-size: 1.125rem;
}

.font-semibold {
  font-weight: 600;
}

.mb-6 {
  margin-bottom: 1.5rem;
}

.text-text {
  color: var(--text);
}

.text-text-secondary {
  color: var(--text-secondary);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>


