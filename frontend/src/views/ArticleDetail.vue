<template>
  <div class="article-detail">
    <!-- 顶部标题和大纲区域 -->
    <div class="header-section">
      <div class="container">
        <div class="header-top">
          <h1 class="article-title">{{ article?.articleName || '加载中...' }}</h1>
          <div class="header-actions">
            <button @click="downloadFullText" class="btn btn-primary download-btn">
              下载全文
            </button>
          </div>
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
                <button @click.stop="deleteChapter(chapter)" class="delete-btn" title="删除章节">×</button>
              </div>
            </div>
          </div>

          <!-- 右侧章节内容 -->
          <div class="chapter-content">
            <div v-if="selectedChapter" class="content-wrapper">
              <h2 class="chapter-title">{{ selectedChapter.chapterTitle }}</h2>

              <!-- 章节信息区域 -->
              <div class="chapter-info">
                <!-- 核心剧情 -->
                <div class="info-section">
                  <div class="info-title">
                    核心剧情
                    <button v-if="!editingCorePlot" @click="startEditingCorePlot" class="edit-btn">编辑</button>
                  </div>
                  <div v-if="!editingCorePlot" class="info-content">{{ selectedChapter.corePlot || '暂无核心剧情信息' }}</div>
                  <div v-else class="edit-content">
                    <textarea
                      v-model="editedCorePlot"
                      placeholder="请输入核心剧情..."
                      class="edit-textarea"
                      rows="4"
                    ></textarea>
                    <div class="edit-actions">
                      <button @click="saveCorePlot" class="btn btn-primary btn-sm">保存</button>
                      <button @click="cancelEditingCorePlot" class="btn btn-outline btn-sm">取消</button>
                    </div>
                  </div>
                </div>

                <!-- 字数预估 -->
                <div class="info-section">
                  <div class="info-title">
                    字数预估
                    <button v-if="!editingWordCount" @click="startEditingWordCount" class="edit-btn">编辑</button>
                  </div>
                  <div v-if="!editingWordCount" class="info-content">{{ selectedChapter.wordCountEstimate || 0 }} 字</div>
                  <div v-else class="edit-content">
                    <input
                      v-model.number="editedWordCount"
                      type="number"
                      placeholder="请输入字数..."
                      class="edit-input"
                      min="0"
                    />
                    <div class="edit-actions">
                      <button @click="saveWordCount" class="btn btn-primary btn-sm">保存</button>
                      <button @click="cancelEditingWordCount" class="btn btn-outline btn-sm">取消</button>
                    </div>
                  </div>
                </div>

                <!-- 伏笔信息 -->
                <div class="info-section">
                  <div class="info-title">
                    伏笔信息
                    <button v-if="!editingPlots" @click="startEditingPlots" class="edit-btn">编辑</button>
                  </div>
                  <div v-if="!editingPlots" class="info-content">
                    <div v-if="selectedChapter.plots && selectedChapter.plots.length > 0">
                      <div v-for="plot in selectedChapter.plots" :key="plot.id" class="plot-item">
                        <div class="plot-name">{{ plot.plotName }}</div>
                        <div class="plot-content">{{ plot.plotContent }}</div>
                        <div class="plot-recovery" v-if="plot.recoveryChapterId">
                          回收章节：第{{ plot.recoveryChapterNo || plot.recoveryChapterId }}章
                        </div>
                      </div>
                    </div>
                    <div v-else>暂无伏笔信息</div>
                  </div>
                  <div v-else class="edit-content">
                    <div v-for="(plot, index) in editedPlots" :key="index" class="plot-edit-item">
                      <div class="plot-form">
                        <input
                          v-model="plot.plotName"
                          placeholder="伏笔名称"
                          class="edit-input plot-input"
                        />
                        <textarea
                          v-model="plot.plotContent"
                          placeholder="伏笔内容描述"
                          class="edit-textarea plot-textarea"
                          rows="2"
                        ></textarea>
                        <input
                          v-model.number="plot.recoveryChapterId"
                          type="number"
                          placeholder="回收章节ID（可选）"
                          class="edit-input plot-input"
                          min="1"
                        />
                        <button @click="removePlot(index)" class="btn btn-danger btn-xs remove-plot-btn">删除</button>
                      </div>
                    </div>
                    <button @click="addPlot" class="btn btn-outline btn-sm add-plot-btn">添加伏笔</button>
                    <div class="edit-actions">
                      <button @click="savePlots" class="btn btn-primary btn-sm">保存</button>
                      <button @click="cancelEditingPlots" class="btn btn-outline btn-sm">取消</button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 章节内容 -->
              <div class="chapter-text" v-html="formatChapterContent(selectedChapter.chapterContent || '暂无章节内容')"></div>
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

interface PlotRespDto {
  id: number;
  plotName: string;
  plotContent: string;
  recoveryChapterId?: number;
  recoveryChapterNo?: number;
}

interface PlotReqDto {
  id?: number;
  plotName: string;
  plotContent: string;
  recoveryChapterId?: number;
}

interface ArticleChapterRespDto {
  id: number;
  chapterNo: number;
  chapterTitle: string;
  chapterContent?: string;
  corePlot?: string;
  wordCountEstimate?: number;
  chapterVoiceLink?: string;
  chapterVideoLink?: string;
  plots?: PlotRespDto[];
}

const route = useRoute();
const router = useRouter();

const article = ref<ArticleRespDto | null>(null);
const chapters = ref<ArticleChapterRespDto[]>([]);
const selectedChapterId = ref<number | null>(null);
const selectedChapter = ref<ArticleChapterRespDto | null>(null);

// 编辑状态管理
const editingCorePlot = ref(false);
const editingWordCount = ref(false);
const editingPlots = ref(false);

// 编辑表单数据
const editedCorePlot = ref('');
const editedWordCount = ref(0);
const editedPlots = ref<PlotRespDto[]>([]);


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

  // 初始化编辑数据
  editedCorePlot.value = chapter.corePlot || '';
  editedWordCount.value = chapter.wordCountEstimate || 0;
  editedPlots.value = chapter.plots ? [...chapter.plots] : [];

  // 重置编辑状态
  editingCorePlot.value = false;
  editingWordCount.value = false;
  editingPlots.value = false;
};

// 格式化章节内容
const formatChapterContent = (content: string) => {
  if (!content) return '暂无内容';
  // 将换行符转换为<br>标签
  return content.replace(/\n/g, '<br>');
};

// 删除章节
const deleteChapter = async (chapter: ArticleChapterRespDto) => {

  try {
    const resp = await http.delete<BaseResponse<boolean>>(`/api/articles/chapters/${chapter.id}`);
    const response = resp.data;
    if (response.code === '00000000') {
      window.showNotification('章节删除成功', 'success');

      // 从列表中移除该章节
      const index = chapters.value.findIndex(c => c.id === chapter.id);
      if (index !== -1) {
        chapters.value.splice(index, 1);
      }

      // 如果删除的是当前选中的章节，清空选中状态
      if (selectedChapterId.value === chapter.id) {
        selectedChapterId.value = null;
        selectedChapter.value = null;
        editedCorePlot.value = '';
        editedWordCount.value = 0;
        editedPlots.value = [];
      }
    } else {
      window.showNotification('删除失败：' + (response.msg || '未知错误'), 'error');
    }
  } catch (error: any) {
    console.error('删除章节失败:', error);
    window.showNotification('删除失败，请稍后重试', 'error');
  }
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
    window.showNotification('下载失败，请稍后重试', 'error');
  }
};

// 编辑功能
const startEditingCorePlot = () => {
  editingCorePlot.value = true;
};

const cancelEditingCorePlot = () => {
  editedCorePlot.value = selectedChapter.value?.corePlot || '';
  editingCorePlot.value = false;
};

const saveCorePlot = async () => {
  if (!selectedChapter.value) return;
  try {
    await http.put(`/api/articles/chapters/${selectedChapter.value.id}`, {
      corePlot: editedCorePlot.value,
    });
    selectedChapter.value.corePlot = editedCorePlot.value;
    editingCorePlot.value = false;
    window.showNotification('核心剧情保存成功', 'success');
  } catch (error) {
    console.error('保存核心剧情失败:', error);
    window.showNotification('保存失败，请稍后重试', 'error');
  }
};

const startEditingWordCount = () => {
  editingWordCount.value = true;
};

const cancelEditingWordCount = () => {
  editedWordCount.value = selectedChapter.value?.wordCountEstimate || 0;
  editingWordCount.value = false;
};

const saveWordCount = async () => {
  if (!selectedChapter.value) return;
  try {
    await http.put(`/api/articles/chapters/${selectedChapter.value.id}`, {
      wordCountEstimate: editedWordCount.value,
    });
    selectedChapter.value.wordCountEstimate = editedWordCount.value;
    editingWordCount.value = false;
    window.showNotification('字数预估保存成功', 'success');
  } catch (error) {
    console.error('保存字数预估失败:', error);
    window.showNotification('保存失败，请稍后重试', 'error');
  }
};

const startEditingPlots = () => {
  editingPlots.value = true;
};

const cancelEditingPlots = () => {
  editedPlots.value = selectedChapter.value?.plots ? [...selectedChapter.value.plots] : [];
  editingPlots.value = false;
};

const addPlot = () => {
  editedPlots.value.push({
    id: Math.random(), // 临时ID，前端使用
    plotName: '',
    plotContent: '',
    recoveryChapterId: undefined,
  });
};

const removePlot = (index: number) => {
  editedPlots.value.splice(index, 1);
};

const savePlots = async () => {
  if (!selectedChapter.value) return;
  try {
    const plotsForApi = editedPlots.value.map(plot => ({
      id: plot.id || undefined,
      plotName: plot.plotName,
      plotContent: plot.plotContent,
      recoveryChapterId: plot.recoveryChapterId,
    }));

    await http.put(`/api/articles/chapters/${selectedChapter.value.id}`, {
      plots: plotsForApi,
    });

    // 更新本地数据
    selectedChapter.value.plots = editedPlots.value.map(plot => ({
      ...plot,
      id: plot.id || Math.random(), // 临时ID，前端使用
    }));

    editingPlots.value = false;
    window.showNotification('伏笔信息保存成功', 'success');
  } catch (error) {
    console.error('保存伏笔信息失败:', error);
    window.showNotification('保存失败，请稍后重试', 'error');
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

.header-actions {
  display: flex;
  gap: 1rem;
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
  position: relative;
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

.delete-btn {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 1.2rem;
  font-weight: bold;
  cursor: pointer;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  margin-left: 0.5rem;
  opacity: 0;
}

.chapter-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background-color: #fee2e2;
  color: #dc2626;
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

/* 章节信息区域 */
.chapter-info {
  background: var(--surface);
  border-radius: 0.75rem;
  padding: 1.5rem;
  margin-bottom: 2rem;
  border: 1px solid var(--border);
}

.info-section {
  margin-bottom: 1.5rem;
}

.info-section:last-child {
  margin-bottom: 0;
}

.info-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: var(--primary);
  border-radius: 2px;
  margin-right: 0.5rem;
}

.info-content {
  font-size: 0.95rem;
  line-height: 1.6;
  color: var(--text);
  background: white;
  padding: 0.75rem;
  border-radius: 0.5rem;
  border: 1px solid var(--border-light, #e5e7eb);
  white-space: pre-wrap;
  word-wrap: break-word;
}

.chapter-text {
  font-size: 1rem;
  line-height: 1.8;
  color: var(--text);
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 伏笔信息样式 */
.plot-item {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: var(--background, #f9fafb);
  border-radius: 0.5rem;
  border: 1px solid var(--border-light, #e5e7eb);
}

.plot-item:last-child {
  margin-bottom: 0;
}

.plot-name {
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 0.5rem;
  font-size: 0.95rem;
}

.plot-content {
  font-size: 0.9rem;
  line-height: 1.5;
  color: var(--text);
  margin-bottom: 0.5rem;
}

.plot-recovery {
  font-size: 0.85rem;
  color: var(--text-secondary, #6b7280);
  font-style: italic;
}

/* 编辑相关样式 */
.edit-btn {
  background: var(--primary);
  color: white;
  border: none;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.8rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.edit-btn:hover {
  background: var(--primary-dark, #1d4ed8);
}

.edit-content {
  margin-top: 0.5rem;
}

.edit-input, .edit-textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 0.5rem;
  font-size: 0.9rem;
  font-family: inherit;
  resize: vertical;
}

.edit-input:focus, .edit-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.edit-actions {
  margin-top: 0.75rem;
  display: flex;
  gap: 0.5rem;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}

.btn-xs {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
}

/* 伏笔编辑样式 */
.plot-edit-item {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: var(--background, #f9fafb);
  border-radius: 0.5rem;
  border: 1px solid var(--border-light, #e5e7eb);
}

.plot-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.plot-input {
  width: auto;
  flex: 1;
}

.plot-textarea {
  resize: vertical;
  min-height: 60px;
}

.remove-plot-btn {
  align-self: flex-start;
  margin-top: 0.25rem;
}

.add-plot-btn {
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

  .header-actions {
    flex-direction: column;
    gap: 0.5rem;
  }
}

/* 弹窗样式 */
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

.modal-content {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-secondary, #6b7280);
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.25rem;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background: var(--background, #f9fafb);
  color: var(--text);
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 0.5rem;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 0.5rem;
  font-size: 0.95rem;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary, #2563eb);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.5rem;
  border-top: 1px solid var(--border-light, #e5e7eb);
}

</style>