<template>
  <div>
    <div class="card mb-6">
      <div class="card-header">
        <h2 class="text-xl font-semibold text-text">字典管理</h2>
      </div>

      <div class="card-body">
        <div class="toolbar">
          <div class="search-section">
            <select
              v-model="searchDictKey"
              class="search-select"
              @change="handleSearch"
            >
              <option value="">全部字典键</option>
              <option
                v-for="key in allDictKeys"
                :key="key"
                :value="key"
              >
                {{ key }}
              </option>
            </select>
            <input
              v-model="searchDictValue"
              type="text"
              class="search-input"
              placeholder="请输入字典值搜索..."
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
              新增字典
            </button>
          </div>
        </div>

        <div class="table-container">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">加载中...</p>
          </div>

          <table v-else-if="dictionaries.length > 0" class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>字典键</th>
                <th>字典值</th>
                <th>排序顺序</th>
                <th>创建时间</th>
                <th class="action-column">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="dictionary in dictionaries" :key="dictionary.id">
                <td>{{ dictionary.id }}</td>
                <td>{{ dictionary.dictKey }}</td>
                <td>{{ dictionary.dictValue }}</td>
                <td>{{ dictionary.sortOrder }}</td>
                <td>{{ formatDate(dictionary.createTime) }}</td>
                <td class="action-column">
                  <div class="flex gap-2">
                    <button @click="editDictionary(dictionary)" class="btn btn-outline btn-sm">
                      编辑
                    </button>
                    <select @change="handleActionSelect($event, dictionary)" class="action-select">
                      <option value="">更多操作</option>
                      <option
                        :value="'delete_' + dictionary.id"
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
            <p class="empty-text">暂无字典数据</p>
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
              @click="goToPage(currentPage - 1)"
              :disabled="currentPage <= 1"
              class="pagination-item"
            >
              上一页
            </button>
            <span class="pagination-item">
              {{ currentPage }} / {{ totalPages }}
            </span>
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="currentPage >= totalPages"
              class="pagination-item"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑模态框 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">{{ showCreateModal ? '新增字典' : '编辑字典' }}</h3>
          <button @click="closeModals" class="modal-close">&times;</button>
        </div>

        <form @submit.prevent="handleSubmit" class="modal-body">
          <div class="form-group">
            <label class="form-label">字典键 <span class="required">*</span></label>
            <input
              v-model="formData.dictKey"
              type="text"
              class="form-input"
              placeholder="请输入字典键"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">字典值 <span class="required">*</span></label>
            <input
              v-model="formData.dictValue"
              type="text"
              class="form-input"
              placeholder="请输入字典值"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">排序顺序</label>
            <input
              v-model.number="formData.sortOrder"
              type="number"
              class="form-input"
              placeholder="请输入排序顺序"
              min="0"
            />
          </div>

            <div class="flex gap-2 justify-end">
              <button type="button" @click="closeModals" class="btn btn-outline">
                取消
              </button>
              <button type="submit" :disabled="submitting" class="btn btn-primary">
                {{ submitting ? '提交中...' : '确定' }}
              </button>
            </div>
        </form>
      </div>
    </div>

    <!-- 删除确认模态框 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click="closeDeleteModal">
      <div class="modal-content modal-small" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">确认删除</h3>
          <button @click="closeDeleteModal" class="modal-close">&times;</button>
        </div>

        <div class="modal-body">
          <p>确定要删除字典 "{{ deleteTarget?.dictKey }}: {{ deleteTarget?.dictValue }}" 吗？此操作不可恢复。</p>
        </div>

          <div class="flex gap-2 justify-end">
            <button @click="closeDeleteModal" class="btn btn-outline">
              取消
            </button>
            <button @click="confirmDelete" :disabled="submitting" class="btn btn-error">
              {{ submitting ? '删除中...' : '删除' }}
            </button>
          </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { http } from '../lib/http/client';
import type { BaseResponse, PageRespDto } from '../lib/types/base';

interface DictionaryListRespDto {
  id: number;
  dictKey: string;
  dictValue: string;
  sortOrder: number;
  createTime: string;
}

const dictionaries = ref<DictionaryListRespDto[]>([]);
const loading = ref(false);
const submitting = ref(false);
const searchDictKey = ref('');
const searchDictValue = ref('');
const allDictKeys = ref<string[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);
const showCreateModal = ref(false);
const showEditModal = ref(false);
const showDeleteModal = ref(false);
const deleteTarget = ref<DictionaryListRespDto | null>(null);
const formData = ref({
  id: null as number | null,
  dictKey: '',
  dictValue: '',
  sortOrder: 0
});


onMounted(() => {
  loadDictionaries();
  loadAllDictKeys();
});

const loadDictionaries = async () => {
  loading.value = true;
  try {
    const resp = await http.post<BaseResponse<PageRespDto<DictionaryListRespDto>>>('/api/dictionaries/list', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      dictKey: searchDictKey.value.trim() || undefined,
      dictValue: searchDictValue.value.trim() || undefined
    });
    const page = resp.data.data;
    dictionaries.value = page?.list || [];
    total.value = page?.total || 0;
    totalPages.value = page?.pages || 0;
    currentPage.value = page?.pageNo || 1;
  } catch (error: any) {
    console.error('加载字典列表失败:', error);
    showError(error.message || '加载数据失败，请重试');
  } finally {
    loading.value = false;
  }
};

const loadAllDictKeys = async () => {
  try {
    const resp = await http.get<BaseResponse<string[]>>('/api/dictionaries/keys');
    allDictKeys.value = resp.data.data || [];
  } catch (error: any) {
    console.error('加载字典键列表失败:', error);
    // 不显示错误提示，因为这不是关键功能
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadDictionaries();
};

const clearSearch = () => {
  searchDictKey.value = '';
  searchDictValue.value = '';
  currentPage.value = 1;
  loadDictionaries();
};

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    loadDictionaries();
  }
};

// 下拉列表操作
const handleActionSelect = async (event: Event, dictionary: DictionaryListRespDto) => {
  const target = event.target as HTMLSelectElement;
  const value = target.value;

  if (!value) return; // 如果选择的是"更多操作"，不做任何操作

  const [action, dictIdStr] = value.split('_');
  const dictId = parseInt(dictIdStr);

  if (action === 'delete') {
    deleteDictionary(dictionary);
  }

  // 重置选择
  target.value = '';
};

const editDictionary = (dictionary: DictionaryListRespDto) => {
  formData.value = {
    id: dictionary.id,
    dictKey: dictionary.dictKey,
    dictValue: dictionary.dictValue,
    sortOrder: dictionary.sortOrder
  };
  showEditModal.value = true;
};

const deleteDictionary = (dictionary: DictionaryListRespDto) => {
  deleteTarget.value = dictionary;
  showDeleteModal.value = true;
};

const closeModals = () => {
  showCreateModal.value = false;
  showEditModal.value = false;
  formData.value = {
    id: null,
    dictKey: '',
    dictValue: '',
    sortOrder: 0
  };
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  deleteTarget.value = null;
};

const handleSubmit = async () => {
  if (!formData.value.dictKey.trim() || !formData.value.dictValue.trim()) {
    showError('请输入字典键和字典值');
    return;
  }

  submitting.value = true;
  try {
    if (showCreateModal.value) {
      // 创建
      await http.post('/api/dictionaries', {
        dictKey: formData.value.dictKey.trim(),
        dictValue: formData.value.dictValue.trim(),
        sortOrder: formData.value.sortOrder || 0
      });
      showSuccess('创建成功');
    } else {
      // 更新
      await http.put('/api/dictionaries', {
        id: formData.value.id,
        dictKey: formData.value.dictKey.trim(),
        dictValue: formData.value.dictValue.trim(),
        sortOrder: formData.value.sortOrder || 0
      });
      showSuccess('更新成功');
    }
    closeModals();
    loadDictionaries();
  } catch (error: any) {
    console.error('操作失败:', error);
    showError(error.message || '操作失败，请重试');
  } finally {
    submitting.value = false;
  }
};

const confirmDelete = async () => {
  if (!deleteTarget.value) return;

  submitting.value = true;
  try {
    await http.delete('/api/dictionaries', { data: { id: deleteTarget.value.id } });
    showSuccess('删除成功');
    closeDeleteModal();
    loadDictionaries();
  } catch (error: any) {
    console.error('删除失败:', error);
    showError(error.message || '删除失败，请重试');
  } finally {
    submitting.value = false;
  }
};

const formatDate = (dateString: string) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const showSuccess = (message: string) => {
  window.showNotification(message, 'success');
};

const showError = (message: string) => {
  window.showNotification('错误: ' + message, 'error');
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

.search-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  font-size: 0.875rem;
  background: white;
  min-width: 120px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}



.action-column {
  width: 120px;
}

.flex {
  display: flex;
}

.gap-2 {
  gap: 0.5rem;
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

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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

.pagination-container {
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

.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
  background: white;
  border-radius: 0.5rem;
  box-shadow: var(--shadow);
}

.modal-small {
  max-width: 400px;
  background: white;
  border-radius: 0.5rem;
  box-shadow: var(--shadow);
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-body {
  padding: 1.5rem;
  background: white;
}

.modal-footer {
  padding: 1.5rem;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  background: white;
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

.required {
  color: var(--error);
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
</style>