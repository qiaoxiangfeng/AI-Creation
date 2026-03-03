<template>
  <div>
    <div class="card mb-6">
      <div class="card-header">
        <h2 class="text-xl font-semibold text-text">文章类型管理</h2>
      </div>

      <div class="card-body">
        <div class="toolbar">
          <div class="search-section">
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="请输入文章类型搜索..."
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
              新增文章类型
            </button>
          </div>
        </div>

        <div class="table-container">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">加载中...</p>
          </div>

          <table v-else-if="articleTypes.length > 0" class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>文章类型</th>
                <th>待生成数量</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="articleType in articleTypes" :key="articleType.id">
                <td>{{ articleType.id }}</td>
                <td>{{ articleType.articleType }}</td>
                <td>{{ articleType.pendingCount }}</td>
                <td>{{ formatDate(articleType.createTime) }}</td>
                <td>
                  <div class="flex gap-2">
                    <button @click="editArticleType(articleType)" class="btn btn-outline btn-sm">
                      编辑
                    </button>
                    <button @click="deleteArticleType(articleType)" class="btn btn-outline btn-sm text-error">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-else class="empty-state">
            <p class="empty-text">暂无文章类型数据</p>
          </div>
        </div>

        <!-- 分页组件 -->
        <div v-if="total > 0" class="pagination-container">
          <div class="pagination-info">
            <span>共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页</span>
          </div>
          <div class="pagination-buttons">
            <button
              @click="goToPage(currentPage - 1)"
              :disabled="currentPage <= 1"
              class="btn btn-outline btn-sm"
            >
              上一页
            </button>
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="currentPage >= totalPages"
              class="btn btn-outline btn-sm"
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
          <h3 class="modal-title">{{ showCreateModal ? '新增文章类型' : '编辑文章类型' }}</h3>
          <button @click="closeModals" class="modal-close">&times;</button>
        </div>

        <form @submit.prevent="handleSubmit" class="modal-body">
          <div class="form-group">
            <label class="form-label">文章类型 <span class="required">*</span></label>
            <input
              v-model="formData.articleType"
              type="text"
              class="form-input"
              placeholder="请输入文章类型"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">待生成数量</label>
            <input
              v-model.number="formData.pendingCount"
              type="number"
              class="form-input"
              placeholder="请输入待生成数量"
              min="0"
            />
          </div>

          <div class="modal-footer">
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
          <p>确定要删除文章类型 "{{ deleteTarget?.articleType }}" 吗？此操作不可恢复。</p>
        </div>

        <div class="modal-footer">
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

interface ArticleTypeRespDto {
  id: number;
  articleType: string;
  pendingCount: number;
  createTime: string;
}

interface ArticleTypeListRespDto {
  id: number;
  articleType: string;
  pendingCount: number;
  createTime: string;
}

const articleTypes = ref<ArticleTypeListRespDto[]>([]);
const loading = ref(false);
const submitting = ref(false);
const searchKeyword = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);
const showCreateModal = ref(false);
const showEditModal = ref(false);
const showDeleteModal = ref(false);
const deleteTarget = ref<ArticleTypeListRespDto | null>(null);
const formData = ref({
  id: null as number | null,
  articleType: '',
  pendingCount: 0
});

onMounted(() => {
  loadArticleTypes();
});

const loadArticleTypes = async () => {
  loading.value = true;
  try {
    const resp = await http.post<BaseResponse<PageRespDto<ArticleTypeListRespDto>>>('/api/article-types/list', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      articleType: searchKeyword.value.trim() || undefined
    });
    const page = resp.data.data;
    articleTypes.value = page?.list || [];
    total.value = page?.total || 0;
    totalPages.value = page?.pages || 0;
    currentPage.value = page?.pageNo || 1;
  } catch (error: any) {
    console.error('加载文章类型列表失败:', error);
    showError(error.message || '加载数据失败，请重试');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadArticleTypes();
};

const clearSearch = () => {
  searchKeyword.value = '';
  currentPage.value = 1;
  loadArticleTypes();
};

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    loadArticleTypes();
  }
};

const editArticleType = (articleType: ArticleTypeListRespDto) => {
  formData.value = {
    id: articleType.id,
    articleType: articleType.articleType,
    pendingCount: articleType.pendingCount
  };
  showEditModal.value = true;
};

const deleteArticleType = (articleType: ArticleTypeListRespDto) => {
  deleteTarget.value = articleType;
  showDeleteModal.value = true;
};

const closeModals = () => {
  showCreateModal.value = false;
  showEditModal.value = false;
  formData.value = {
    id: null,
    articleType: '',
    pendingCount: 0
  };
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  deleteTarget.value = null;
};

const handleSubmit = async () => {
  if (!formData.value.articleType.trim()) {
    showError('请输入文章类型');
    return;
  }

  submitting.value = true;
  try {
    if (showCreateModal.value) {
      // 创建
      await http.post('/api/article-types', {
        articleType: formData.value.articleType.trim(),
        pendingCount: formData.value.pendingCount || 0
      });
      showSuccess('创建成功');
    } else {
      // 更新
      await http.put('/api/article-types', {
        id: formData.value.id,
        articleType: formData.value.articleType.trim(),
        pendingCount: formData.value.pendingCount || 0
      });
      showSuccess('更新成功');
    }
    closeModals();
    loadArticleTypes();
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
    await http.delete('/api/article-types', { data: { id: deleteTarget.value.id } });
    showSuccess('删除成功');
    closeDeleteModal();
    loadArticleTypes();
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
  alert(message);
};

const showError = (message: string) => {
  alert('错误: ' + message);
};
</script>

<style scoped>
/* 复用Articles.vue的样式，这里只展示基本的样式结构 */
.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.card-body {
  padding: 1.5rem;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1rem;
}

.search-section {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.search-input {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  width: 250px;
}

.table-container {
  margin-bottom: 1.5rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.table th,
.table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #374151;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5px;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background-color: #2563eb;
}

.btn-outline {
  background-color: transparent;
  border: 1px solid #d1d5db;
  color: #374151;
}

.btn-outline:hover {
  background-color: #f9fafb;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.flex {
  display: flex;
}

.gap-2 {
  gap: 0.5rem;
}

.text-error {
  color: #dc2626;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 1rem;
  color: #6b7280;
}

.empty-state {
  text-align: center;
  padding: 3rem;
}

.empty-text {
  color: #6b7280;
  font-size: 1rem;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
}

.pagination-info {
  color: #6b7280;
  font-size: 0.875rem;
}

.pagination-buttons {
  display: flex;
  gap: 0.5rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-small {
  max-width: 400px;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #6b7280;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.5rem;
}

.required {
  color: #dc2626;
}

.form-input {
  width: 100%;
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.btn-error {
  background-color: #dc2626;
  color: white;
}

.btn-error:hover {
  background-color: #b91c1c;
}
</style>