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
                <th>文章类型</th>
                <th>文章特点</th>
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
                <td>{{ article.articleType || '-' }}</td>
                <td>
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
                    <button @click="viewArticle(article)" class="btn btn-outline btn-sm">
                      查看
                    </button>
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
              <label class="form-label">故事背景</label>
              <textarea v-model="newArticle.storyBackground" class="form-textarea" rows="4" placeholder="请输入故事背景..."></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">文章类型</label>
              <input v-model="newArticle.articleType" class="form-input" placeholder="请输入文章类型" />
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
              <label class="form-label">故事背景</label>
              <textarea v-model="editingArticle.storyBackground" class="form-textarea" rows="4" placeholder="请输入故事背景..."></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">文章类型</label>
              <input v-model="editingArticle.articleType" class="form-input" placeholder="请输入文章类型" />
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

            <!-- 总字数预估 -->
            <div class="form-group">
              <label class="form-label">总字数预估 *</label>
              <div class="input-with-buttons">
                <button type="button" @click="adjustTotalWordCount(-1000)" class="btn-adjust">-</button>
                <input
                  v-model.number="editingArticle.totalWordCountEstimate"
                  type="number"
                  class="form-input form-input-number"
                  min="1000"
                  max="500000"
                  step="1000"
                  required
                  placeholder="请输入总字数预估"
                />
                <button type="button" @click="adjustTotalWordCount(1000)" class="btn-adjust">+</button>
              </div>
              <div class="form-hint">每次调整1000字</div>
            </div>

            <!-- 每章节字数预估 -->
            <div class="form-group">
              <label class="form-label">每章节字数预估 *</label>
              <div class="input-with-buttons">
                <button type="button" @click="adjustChapterWordCount(-100)" class="btn-adjust">-</button>
                <input
                  v-model.number="editingArticle.chapterWordCountEstimate"
                  type="number"
                  class="form-input form-input-number"
                  min="100"
                  max="10000"
                  step="100"
                  required
                  placeholder="请输入每章节字数预估"
                />
                <button type="button" @click="adjustChapterWordCount(100)" class="btn-adjust">+</button>
              </div>
              <div class="form-hint">每次调整100字</div>
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
import { useRouter } from 'vue-router';
import { http } from '../lib/http/client';
import type { BaseResponse, PageRespDto } from '../lib/types/base';

const router = useRouter();

interface ArticleRespDto {
  id: number;
  articleName: string;
  articleOutline?: string;
  storyBackground?: string;
  imageDesc?: string;
  articleType?: string;
  voiceTone?: string;
  voiceLink?: string;
  voiceFilePath?: string;
  videoLink?: string;
  videoFilePath?: string;
  publishStatus: number;
  totalWordCountEstimate?: number;
  chapterWordCountEstimate?: number;
  generationStatus?: number;
  resState: number;
  createTime: string;
  updateTime: string;
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
  storyBackground: '',
  articleType: '',
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
    const resp = await http.post<BaseResponse<PageRespDto<ArticleRespDto>>>('/api/articles/list', {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      articleName: searchKeyword.value.trim() || undefined,
      voiceTone: searchVoiceTone.value || undefined,
    });
    const page = resp.data.data;
    articles.value = page?.list || [];
    total.value = page?.total || 0;
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
      storyBackground: '',
      articleType: '',
      voiceTone: '',
      voiceLink: '',
      voiceFilePath: '',
      videoLink: '',
      videoFilePath: ''
    };
    loadArticles();
    window.showNotification('文章创建成功！', 'success');
  } catch (error: any) {
    console.error('创建文章失败:', error);
    const errorMessage = error.message || '创建文章失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

// 调整总字数预估
const adjustTotalWordCount = (delta: number) => {
  if (!editingArticle.value) return;
  const current = editingArticle.value.totalWordCountEstimate || 0;
  const newValue = Math.max(1000, Math.min(500000, current + delta));
  editingArticle.value.totalWordCountEstimate = newValue;
};

// 调整每章节字数预估
const adjustChapterWordCount = (delta: number) => {
  if (!editingArticle.value) return;
  const current = editingArticle.value.chapterWordCountEstimate || 0;
  const newValue = Math.max(100, Math.min(10000, current + delta));
  editingArticle.value.chapterWordCountEstimate = newValue;
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
      storyBackground: editingArticle.value.storyBackground,
      articleType: editingArticle.value.articleType,
      voiceTone: editingArticle.value.voiceTone,
      voiceLink: editingArticle.value.voiceLink,
      voiceFilePath: editingArticle.value.voiceFilePath,
      videoLink: editingArticle.value.videoLink,
      videoFilePath: editingArticle.value.videoFilePath,
      totalWordCountEstimate: editingArticle.value.totalWordCountEstimate,
      chapterWordCountEstimate: editingArticle.value.chapterWordCountEstimate
    };
    
    await http.put('/api/articles', updateData);
    showEditModal.value = false;
    editingArticle.value = null;
    loadArticles();
    window.showNotification('文章更新成功！', 'success');
  } catch (error: any) {
    console.error('更新文章失败:', error);
    const errorMessage = error.message || '更新文章失败，请稍后重试';
    alert(errorMessage);
  }
};

const deleteArticle = async (article: ArticleRespDto) => {
  
  try {
    await http.delete('/api/articles', { data: { articleId: article.id } });
    loadArticles();
    window.showNotification('文章删除成功！', 'success');
  } catch (error: any) {
    console.error('删除文章失败:', error);
    const errorMessage = error.message || '删除文章失败，请稍后重试';
    window.showNotification(errorMessage, 'error');
  }
};

const publishArticle = async (article: ArticleRespDto) => {
  const newStatus = article.publishStatus === 1 ? 2 : 1; // 1 for unpublished, 2 for published
  const actionText = newStatus === 2 ? '发布' : '取消发布';
  
  
  try {
    await http.put(`/api/articles/${article.id}/publish?publishStatus=${newStatus}`);
    loadArticles();
    window.showNotification(`文章${actionText}成功！`, 'success');
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

// 查看文章详情
const viewArticle = (article: ArticleRespDto) => {
  // 跳转到文章详情页面
  router.push(`/articles/${article.id}`);
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

/* 数字输入框样式 */
.input-with-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-input-number {
  flex: 1;
  text-align: center;
}

.btn-adjust {
  width: 2.5rem;
  height: 2.5rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background: white;
  color: var(--primary);
  font-size: 1.25rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-adjust:hover {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.btn-adjust:active {
  transform: scale(0.95);
}

.form-hint {
  margin-top: 0.25rem;
  font-size: 0.75rem;
  color: var(--text-secondary);
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

/* 文章特点标签样式 */
.characteristics-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.tag {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  background-color: var(--primary-light, #e0f2fe);
  color: var(--primary, #2563eb);
  font-size: 0.75rem;
  border-radius: 0.25rem;
  border: 1px solid var(--primary-light, #bae6fd);
}
</style>
