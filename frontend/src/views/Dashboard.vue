<template>
  <div class="dashboard">
    <div class="header">
      <h1 class="text-2xl font-bold text-text">文章生成仪表盘</h1>
      <p class="text-text-secondary mt-2">实时监控文章生成进度</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon in-progress">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.inProgress }}</div>
          <div class="stat-label">进行中</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon completed">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.completed }}</div>
          <div class="stat-label">已完结</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon total">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">文章总数</div>
        </div>
      </div>
    </div>

    <!-- 文章列表 -->
    <div class="articles-section">
      <div class="section-header">
        <h2 class="text-xl font-semibold text-text">文章生成进度</h2>
        <div class="header-actions">
          <button @click="loadArticles" class="btn btn-outline btn-sm">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            刷新
          </button>
        </div>
      </div>

      <div class="articles-grid">
        <div v-for="article in sortedArticles" :key="article.id" class="article-card">
          <div class="article-header">
            <h3 class="article-title">{{ article.articleName }}</h3>
            <div class="status-badge" :class="getStatusClass(article)">
              {{ getStatusText(article) }}
            </div>
          </div>

          <div class="article-content">
            <p class="article-outline">{{ article.articleOutline || '暂无简介' }}</p>


            <div class="article-stats">
              <div class="stat-item">
                <span class="stat-label">总字数预估</span>
                <span class="stat-value">{{ article.totalWordCountEstimate || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">每章字数</span>
                <span class="stat-value">{{ article.chapterWordCountEstimate || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">创建时间</span>
                <span class="stat-value">{{ formatDate(article.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="article-actions">
            <button @click="viewArticle(article)" class="btn btn-outline btn-sm">
              查看详情
            </button>
          </div>
        </div>

        <div v-if="articles.length === 0" class="empty-state">
          <svg class="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <p class="empty-text">暂无文章数据</p>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p class="loading-text">加载中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../lib/http/client';
import type { BaseResponse, PageRespDto } from '../lib/types/base';

interface ArticleRespDto {
  id: number;
  articleName: string;
  articleOutline: string;
  totalWordCountEstimate?: number;
  chapterWordCountEstimate?: number;
  storyComplete?: boolean;
  createTime: string;
}

const router = useRouter();
const loading = ref(false);
const articles = ref<ArticleRespDto[]>([]);

// 统计数据
const stats = computed(() => {
  const completed = articles.value.filter(a => a.storyComplete).length;
  const inProgress = articles.value.filter(a => !a.storyComplete).length;

  return { completed, inProgress, total: articles.value.length };
});

// 排序后的文章列表（未生成的排在前面）
const sortedArticles = computed(() => {
  return [...articles.value].sort((a, b) => {
    // 未完结的排在前面
    if (!a.storyComplete && b.storyComplete) return -1;
    if (a.storyComplete && !b.storyComplete) return 1;

    // 按创建时间倒序
    return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
  });
});

// 加载文章列表
const loadArticles = async () => {
  loading.value = true;
  try {
    const resp = await http.post<BaseResponse<PageRespDto<ArticleRespDto>>>('/api/articles/list', {
      pageSize: 100 // 获取所有文章
    });
    const response = resp.data;
    if (response.code === '00000000') {
      const articleList = response.data.list;

      articles.value = articleList;
    }
  } catch (error) {
    console.error('加载文章列表失败:', error);
  } finally {
    loading.value = false;
  }
};

// 获取状态文本
const getStatusText = (article: ArticleRespDto): string => {
  if (article.storyComplete) {
    return '已完结';
  }
  return '进行中';
};

// 获取状态样式类
const getStatusClass = (article: ArticleRespDto): string => {
  if (article.storyComplete) {
    return 'status-completed';
  }
  return 'status-pending';
};


// 查看文章详情
const viewArticle = (article: ArticleRespDto) => {
  router.push(`/articles/${article.id}`);
};


// 格式化日期
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 页面加载时获取数据
onMounted(() => {
  loadArticles();
});
</script>

<style scoped>
.dashboard {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  margin-bottom: 2rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.stat-card {
  background: white;
  border-radius: 0.75rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.in-progress { background-color: #f59e0b; }
.stat-icon.completed { background-color: #10b981; }
.stat-icon.total { background-color: #6b7280; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

.articles-section {
  background: white;
  border-radius: 0.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.articles-grid {
  padding: 1.5rem;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
}

.article-card {
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  padding: 1.5rem;
  transition: box-shadow 0.2s;
}

.article-card:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.article-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  margin-right: 1rem;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.status-pending { background-color: #fef3c7; color: #f59e0b; }
.status-generating { background-color: #dbeafe; color: #3b82f6; }
.status-completed { background-color: #d1fae5; color: #10b981; }
.status-failed { background-color: #fee2e2; color: #ef4444; }

.article-outline {
  color: #6b7280;
  font-size: 0.875rem;
  line-height: 1.5;
  margin-bottom: 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.progress-section {
  margin-bottom: 1rem;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.progress-bar {
  height: 0.5rem;
  background-color: #e5e7eb;
  border-radius: 9999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #3b82f6;
  transition: width 0.3s ease;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.5rem;
}

.article-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.75rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 0.875rem;
  font-weight: 500;
  color: #1f2937;
}

.article-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 3rem;
  color: #6b7280;
}

.empty-text {
  font-size: 1.125rem;
  margin-top: 1rem;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.loading-spinner {
  width: 3rem;
  height: 3rem;
  border: 0.25rem solid #e5e7eb;
  border-top: 0.25rem solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  color: #6b7280;
  font-size: 0.875rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard {
    padding: 1rem;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }

  .article-stats {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
}
</style>