<template>
  <div>
    <div class="card mb-6">
      <div class="card-header">
        <h2 class="text-xl font-semibold text-text">文章管理</h2>
      </div>
      
      <div class="card-body">
        <div class="toolbar">
          <div class="search-section">
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="请输入文章名称搜索..."
              @keyup.enter="handleSearch"
            />
            <select v-model="searchVoiceTone" class="search-select">
              <option value="">所有音色</option>
              <option value="alex">alex</option>
              <option value="anna">anna</option>
            </select>
            <button @click="handleSearch" class="btn btn-primary">
              搜索
            </button>
            <button @click="clearSearch" class="btn btn-outline">
              清空
            </button>
          </div>
          
          <div class="action-buttons">
            <button @click="showCreateModal = true" class="btn btn-primary">
              新增文章
            </button>
          </div>
        </div>
        
        <div class="table-container">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">加载中...</p>
          </div>
          
          <table v-else-if="articles.length > 0" class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>文章名称</th>
                <th>文章简介</th>
                <th>音色</th>
                <th>语音链接</th>
                <th>视频链接</th>
                <th>发布状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="article in articles" :key="article.id">
                <td>{{ article.id }}</td>
                <td>{{ article.articleName }}</td>
                <td class="outline-cell">
                  <div class="outline-text" :title="article.articleOutline || ''">
                    {{ truncateText(article.articleOutline || '', 50) }}
                  </div>
                </td>
                <td>{{ getVoiceToneText(article.voiceTone) }}</td>
                <td>
                  <a v-if="article.voiceLink" :href="article.voiceLink" target="_blank" class="link-text">
                    查看链接
                  </a>
                  <span v-else>-</span>
                </td>
                <td>
                  <a v-if="article.videoLink" :href="article.videoLink" target="_blank" class="link-text">
                    查看链接
                  </a>
                  <span v-else>-</span>
                </td>
                <td>
                  <span :class="getPublishStatusBadgeClass(article.publishStatus)">
                    {{ getPublishStatusText(article.publishStatus) }}
                  </span>
                </td>
                <td>{{ formatDate(article.createTime) }}</td>
                <td>
                  <div class="flex gap-2">
                    <button @click="editArticle(article)" class="btn btn-outline btn-sm">
                      编辑
                    </button>
                    <button 
                      v-if="article.publishStatus !== 2" 
                      @click="publishArticle(article)" 
                      class="btn btn-primary btn-sm"
                    >
                      发布
                    </button>
                    <button @click="deleteArticle(article)" class="btn btn-outline btn-sm text-error">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          
          <div v-else class="empty-state">
            <p class="empty-text">暂无文章数据</p>
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
    
    <!-- 创建文章模态框 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="showCreateModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">新增文章</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="createArticle" class="space-y-4">
            <div class="form-group">
              <label class="form-label">文章名称 *</label>
              <input v-model="newArticle.articleName" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">文章简介</label>
              <textarea v-model="newArticle.articleOutline" class="form-textarea" rows="4" placeholder="请输入文章简介..."></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">音色</label>
              <select v-model="newArticle.voiceTone" class="form-select">
                <option value="">请选择音色</option>
                <option value="alex">alex</option>
                <option value="anna">anna</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">语音链接</label>
              <input v-model="newArticle.voiceLink" class="form-input" placeholder="https://example.com/voice.mp3" />
            </div>
            <div class="form-group">
              <label class="form-label">语音文件地址</label>
              <input v-model="newArticle.voiceFilePath" class="form-input" placeholder="/uploads/voice/voice.mp3" />
            </div>
            <div class="form-group">
              <label class="form-label">视频链接</label>
              <input v-model="newArticle.videoLink" class="form-input" placeholder="https://example.com/video.mp4" />
            </div>
            <div class="form-group">
              <label class="form-label">视频文件地址</label>
              <input v-model="newArticle.videoFilePath" class="form-input" placeholder="/uploads/video/video.mp4" />
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
    
    <!-- 编辑文章模态框 -->
    <div v-if="showEditModal && editingArticle" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">编辑文章</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="updateArticle" class="space-y-4">
            <div class="form-group">
              <label class="form-label">文章名称 *</label>
              <input v-model="editingArticle.articleName" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">文章简介</label>
              <textarea v-model="editingArticle.articleOutline" class="form-textarea" rows="4" placeholder="请输入文章简介..."></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">音色</label>
              <select v-model="editingArticle.voiceTone" class="form-select">
                <option value="">请选择音色</option>
                <option value="alex">alex</option>
                <option value="anna">anna</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">语音链接</label>
              <input v-model="editingArticle.voiceLink" class="form-input" placeholder="https://example.com/voice.mp3" />
            </div>
            <div class="form-group">
              <label class="form-label">语音文件地址</label>
              <input v-model="editingArticle.voiceFilePath" class="form-input" placeholder="/uploads/voice/voice.mp3" />
            </div>
            <div class="form-group">
              <label class="form-label">视频链接</label>
              <input v-model="editingArticle.videoLink" class="form-input" placeholder="https://example.com/video.mp4" />
            </div>
            <div class="form-group">
              <label class="form-label">视频文件地址</label>
              <input v-model="editingArticle.videoFilePath" class="form-input" placeholder="/uploads/video/video.mp4" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { http } from '../lib/http/client';
import type { PageRespDto } from '../lib/types/base';

interface ArticleRespDto {
  id: number;
  articleName: string;
  articleOutline?: string;
  voiceTone?: string;
  voiceLink?: string;
  voiceFilePath?: string;
  videoLink?: string;
  videoFilePath?: string;
  resState: number;
  createTime: string;
  updateTime: string;
  publishStatus: number; // Added publishStatus
}

const articles = ref<ArticleRespDto[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const searchKeyword = ref('');
const searchVoiceTone = ref('');

const showCreateModal = ref(false);
const showEditModal = ref(false);

const newArticle = ref({
  articleName: '',
  articleOutline: '',
  voiceTone: '',
  voiceLink: '',
  voiceFilePath: '',
  videoLink: '',
  videoFilePath: ''
});

const editingArticle = ref<ArticleRespDto | null>(null);

const totalPages = computed(() => Math.ceil(total.value / pageSize.value));

onMounted(() => {
  loadArticles();
});

const loadArticles = async () => {
  loading.value = true;
  try {
    const resp = await http.post<PageRespDto<ArticleRespDto>>('/api/articles/list', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      articleName: searchKeyword.value.trim() || undefined,
      voiceTone: searchVoiceTone.value || undefined,
    });
    articles.value = resp.data.list || [];
    total.value = resp.data.total || 0;
  } catch (error: any) {
    console.error('加载文章列表失败:', error);
    const errorMessage = error.message || '加载文章列表失败，请稍后重试';
    alert(errorMessage);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadArticles();
};

const clearSearch = () => {
  searchKeyword.value = '';
  searchVoiceTone.value = '';
  currentPage.value = 1;
  loadArticles();
};

const handlePageSizeChange = () => {
  currentPage.value = 1;
  loadArticles();
};

const changePage = (page: number) => {
  currentPage.value = page;
  loadArticles();
};

const createArticle = async () => {
  try {
    await http.post('/api/articles', newArticle.value);
    showCreateModal.value = false;
    newArticle.value = {
      articleName: '',
      articleOutline: '',
      voiceTone: '',
      voiceLink: '',
      voiceFilePath: '',
      videoLink: '',
      videoFilePath: ''
    };
    loadArticles();
    alert('文章创建成功！');
  } catch (error: any) {
    console.error('创建文章失败:', error);
    const errorMessage = error.message || '创建文章失败，请稍后重试';
    alert(errorMessage);
  }
};

const editArticle = (article: ArticleRespDto) => {
  editingArticle.value = { ...article };
  showEditModal.value = true;
};

const updateArticle = async () => {
  if (!editingArticle.value) return;
  
  try {
    const updateData = {
      articleId: editingArticle.value.id,
      articleName: editingArticle.value.articleName,
      articleOutline: editingArticle.value.articleOutline,
      voiceTone: editingArticle.value.voiceTone,
      voiceLink: editingArticle.value.voiceLink,
      voiceFilePath: editingArticle.value.voiceFilePath,
      videoLink: editingArticle.value.videoLink,
      videoFilePath: editingArticle.value.videoFilePath
    };
    
    await http.put('/api/articles', updateData);
    showEditModal.value = false;
    editingArticle.value = null;
    loadArticles();
    alert('文章更新成功！');
  } catch (error: any) {
    console.error('更新文章失败:', error);
    const errorMessage = error.message || '更新文章失败，请稍后重试';
    alert(errorMessage);
  }
};

const deleteArticle = async (article: ArticleRespDto) => {
  if (!confirm(`确定要删除文章 "${article.articleName}" 吗？`)) return;
  
  try {
    await http.delete('/api/articles', { data: { articleId: article.id } });
    loadArticles();
    alert('文章删除成功！');
  } catch (error: any) {
    console.error('删除文章失败:', error);
    const errorMessage = error.message || '删除文章失败，请稍后重试';
    alert(errorMessage);
  }
};

const publishArticle = async (article: ArticleRespDto) => {
  const newStatus = article.publishStatus === 1 ? 2 : 1; // 1 for unpublished, 2 for published
  const actionText = newStatus === 2 ? '发布' : '取消发布';
  
  if (!confirm(`确定要${actionText}文章 "${article.articleName}" 吗？`)) return;
  
  try {
    await http.put(`/api/articles/${article.id}/publish?publishStatus=${newStatus}`);
    loadArticles();
    alert(`文章${actionText}成功！`);
  } catch (error: any) {
    console.error(`${actionText}文章失败:`, error);
    const errorMessage = error.message || `${actionText}文章失败，请稍后重试`;
    alert(errorMessage);
  }
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-CN');
};

const getPublishStatusText = (status: number) => {
  return status === 2 ? '已发布' : '未发布';
};

const getPublishStatusBadgeClass = (status: number) => {
  return status === 2 ? 'badge badge-success' : 'badge badge-warning';
};

const getVoiceToneText = (tone: string | undefined) => {
  if (!tone) return '-';
  if (tone === 'alex') return 'Alex';
  if (tone === 'anna') return 'Anna';
  return tone;
};

const truncateText = (text: string, maxLength: number) => {
  if (!text) return '-';
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
};
</script>

<style scoped>
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

.table-container {
  overflow-x: auto;
}

.outline-cell {
  max-width: 200px;
}

.outline-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.link-text {
  color: var(--primary);
  text-decoration: none;
}

.link-text:hover {
  text-decoration: underline;
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
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.space-y-4 > * + * {
  margin-top: 1rem;
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

.form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  background: white;
}

.form-select:focus {
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

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
