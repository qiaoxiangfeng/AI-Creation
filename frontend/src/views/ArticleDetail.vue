<template>
  <div class="article-detail">
    <!-- 顶部标题和大纲区域 -->
    <div class="header-section">
      <div class="container">
        <div class="header-top">
          <h1 class="article-title">{{ article?.articleName || '加载中...' }}</h1>
          <button @click="downloadFullText" class="btn btn-primary download-btn">
            下载全文
          </button>
        </div>
        <div class="article-outline">
          <h3>故事大纲</h3>
          <p>{{ article?.articleOutline || '暂无大纲' }}</p>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-section">
      <div class="container">
        <div class="content-layout">
          <!-- 左侧章节列表 -->
          <div class="chapters-sidebar">
            <h3>章节列表</h3>
            <div class="chapters-list">
              <div
                v-for="chapter in chapters"
                :key="chapter.id"
                :class="['chapter-item', { active: selectedChapterId === chapter.id }]"
                @click="selectChapter(chapter)"
              >
                <span class="chapter-no">第{{ chapter.chapterNo }}章</span>
                <span class="chapter-title">{{ chapter.chapterTitle }}</span>
              </div>
            </div>
          </div>

          <!-- 右侧章节内容 -->
          <div class="chapter-content">
            <div v-if="selectedChapter" class="content-wrapper">
              <h2 class="chapter-title">{{ selectedChapter.chapterTitle }}</h2>
              <div class="chapter-text" v-html="formatChapterContent(selectedChapter.chapterContent || '')"></div>
            </div>
            <div v-else class="loading-content">
              <div class="loading-spinner"></div>
              <p>加载章节内容中...</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 返回按钮 -->
    <div class="back-section">
      <div class="container">
        <button @click="goBack" class="btn btn-outline">
          返回文章列表
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http } from '../lib/http/client';
import type { BaseResponse } from '../lib/types/base';

interface ArticleRespDto {
  id: number;
  articleName: string;
  articleOutline: string;
  articleType?: string;
  articleCharacteristics?: string;
  voiceTone?: string;
  voiceLink?: string;
  videoLink?: string;
  publishStatus: number;
  createTime: string;
}

interface ArticleChapterRespDto {
  id: number;
  chapterNo: number;
  chapterTitle: string;
  chapterContent?: string;
  chapterVoiceLink?: string;
  chapterVideoLink?: string;
}

interface ArticleChapterRespDto {
  id: number;
  chapterNo: number;
  chapterTitle: string;
  chapterContent?: string;
  chapterVoiceLink?: string;
  chapterVideoLink?: string;
}

const route = useRoute();
const router = useRouter();

const article = ref<ArticleRespDto | null>(null);
const chapters = ref<ArticleChapterRespDto[]>([]);
const selectedChapterId = ref<number | null>(null);
const selectedChapter = ref<ArticleChapterRespDto | null>(null);

// 获取文章详情
const fetchArticleDetail = async (articleId: string) => {
  try {
    const resp = await http.get<BaseResponse<ArticleRespDto>>(`/api/articles/${articleId}`);
    const response = resp.data;
    if (response.code === '00000000') {
      article.value = response.data;
    }
  } catch (error) {
    console.error('获取文章详情失败:', error);
  }
};

// 获取文章章节列表
const fetchArticleChapters = async (articleId: string) => {
  try {
    const resp = await http.get<BaseResponse<ArticleChapterRespDto[]>>(`/api/articles/${articleId}/chapters`);
    const response = resp.data;
    if (response.code === '00000000') {
      chapters.value = response.data || [];
      // 默认选择第一章
      if (chapters.value.length > 0) {
        selectChapter(chapters.value[0]);
      }
    }
  } catch (error) {
    console.error('获取文章章节失败:', error);
  }
};

// 选择章节
const selectChapter = (chapter: ArticleChapterRespDto) => {
  selectedChapterId.value = chapter.id;
  selectedChapter.value = chapter;
};

// 格式化章节内容
const formatChapterContent = (content: string) => {
  if (!content) return '暂无内容';
  // 将换行符转换为<br>标签
  return content.replace(/\n/g, '<br>');
};

// 下载全文
const downloadFullText = async () => {
  if (!article.value) return;

  try {
    const resp = await http.get<BaseResponse<string>>(`/api/articles/${article.value.id}/full-text`);
    const response = resp.data;
    if (response.code === '00000000') {
      const fullText = response.data;
      const blob = new Blob([fullText], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${article.value.articleName}.txt`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  } catch (error) {
    console.error('下载全文失败:', error);
    alert('下载失败，请稍后重试');
  }
};

// 返回上一页
const goBack = () => {
  router.go(-1);
};

onMounted(() => {
  const articleId = route.params.id as string;
  if (articleId) {
    fetchArticleDetail(articleId);
    fetchArticleChapters(articleId);
  }
});
</script>

<style scoped>
.article-detail {
  min-height: 100vh;
  background-color: var(--background, #f8fafc);
}

/* 顶部标题和大纲区域 */
.header-section {
  background: linear-gradient(135deg, var(--primary, #2563eb), var(--primary-dark, #1d4ed8));
  color: white;
  padding: 2rem 0;
  margin-bottom: 2rem;
}

.header-section .container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.download-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  transition: all 0.2s ease;
}

.download-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
}

.article-title {
  font-size: 2.5rem;
  font-weight: bold;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.article-outline {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 1rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.article-outline h3 {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.article-outline p {
  font-size: 1rem;
  line-height: 1.6;
  opacity: 0.9;
}

/* 主要内容区域 */
.content-section {
  padding: 0 0 2rem 0;
}

.content-section .container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.content-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 2rem;
  min-height: 600px;
}

/* 左侧章节列表 */
.chapters-sidebar {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  height: fit-content;
  max-height: 600px;
  overflow-y: auto;
}

.chapters-sidebar h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--text);
  border-bottom: 2px solid var(--primary);
  padding-bottom: 0.5rem;
}

.chapters-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 0.75rem;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.chapter-item:hover {
  background-color: var(--primary-light, #f0f9ff);
  border-color: var(--primary, #2563eb);
}

.chapter-item.active {
  background-color: var(--primary, #2563eb);
  color: white;
  border-color: var(--primary, #2563eb);
}

.chapter-no {
  font-weight: 600;
  margin-right: 0.75rem;
  min-width: 60px;
}

.chapter-title {
  flex: 1;
  font-size: 0.875rem;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧章节内容 */
.chapter-content {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  padding: 2rem;
  min-height: 600px;
  overflow-y: auto;
}

.content-wrapper h2 {
  font-size: 1.75rem;
  font-weight: bold;
  margin-bottom: 1.5rem;
  color: var(--text);
  border-bottom: 2px solid var(--primary);
  padding-bottom: 0.5rem;
}

.chapter-text {
  font-size: 1rem;
  line-height: 1.8;
  color: var(--text);
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 加载状态 */
.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border);
  border-top: 4px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

/* 返回按钮区域 */
.back-section {
  padding: 2rem 0;
  background: white;
  border-top: 1px solid var(--border);
}

.back-section .container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-layout {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .chapters-sidebar {
    order: 2;
    max-height: 300px;
  }

  .chapter-content {
    order: 1;
  }

  .article-title {
    font-size: 2rem;
  }
}

@media (max-width: 640px) {
  .header-section {
    padding: 1.5rem 0;
  }

  .article-title {
    font-size: 1.75rem;
  }

  .content-section,
  .back-section {
    padding: 1rem 0;
  }

  .chapter-content {
    padding: 1.5rem;
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>