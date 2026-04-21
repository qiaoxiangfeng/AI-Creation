<template>
  <div class="article-detail">
    <!-- 顶部标题和大纲区域 -->
    <div class="header-section">
      <div class="container">
        <div class="header-top">
          <h1 class="article-title">{{ article?.articleName || '加载中...' }}</h1>
          <div class="header-actions">
            <button
              v-if="article"
              @click="openGenerateChaptersDialog"
              class="btn btn-secondary download-btn"
              :disabled="aiDisabled"
            >
              按需生成章节
            </button>
            <button
              v-if="article"
              @click="openGenerateChapterContentDialog"
              class="btn btn-secondary download-btn"
              :disabled="aiDisabled"
            >
              按需生成章节内容
            </button>
            <button @click="downloadFullText" class="btn btn-primary download-btn">
              下载全文
            </button>
          </div>
        </div>
        <div class="article-outline">
          <div class="outline-header">
            <h3>故事大纲</h3>
            <button
              v-if="article"
              class="btn btn-secondary btn-sm"
              :disabled="aiDisabled"
              @click="openRefineOutlineDialog"
            >
              AI修大纲
            </button>
          </div>
          <p>{{ article?.articleOutline || '暂无大纲' }}</p>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-section">
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
                <div class="chapter-title-container">
                  <span class="chapter-title">
                    {{ truncateTitle(chapter.chapterTitle, 10) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧章节内容 -->
          <div class="chapter-content">
            <div v-if="selectedChapter" class="content-wrapper">
              <div class="content-header">
                <h2 class="chapter-title">{{ selectedChapter.chapterTitle }}</h2>
                <div class="content-header-actions">
                  <button
                    class="btn btn-secondary chapter-download-btn"
                    @click="downloadCurrentChapter"
                  >
                    下载本章
                  </button>
                  <button
                    v-if="!selectedChapter.chapterContent && selectedChapter.generationStatus !== 1"
                    class="btn btn-secondary"
                    :disabled="aiDisabled || generatingChapters.has(selectedChapter.id)"
                    @click="generateChapterContent(selectedChapter)"
                  >
                    {{ generatingChapters.has(selectedChapter.id) ? '生成中...' : '生成本章内容' }}
                  </button>
                  <button
                    v-else
                    class="btn btn-secondary"
                    :disabled="aiDisabled"
                    @click="openRegenerateDialog(selectedChapter)"
                  >
                    重新生成本章内容
                  </button>
                  <button
                    class="btn btn-outline btn-error"
                    @click="deleteChapter(selectedChapter)"
                  >
                    删除本章
                  </button>
                </div>
              </div>

              <!-- 章节信息区域：可折叠，有内容时默认折叠，无内容时默认展开 -->
              <div class="chapter-info" :class="{ collapsed: isChapterInfoCollapsed }">
                <!-- 折叠切换按钮（右上角） -->
                <div class="chapter-info-toggle" @click="toggleChapterInfo">
                  <span>{{ isChapterInfoCollapsed ? '展开章节信息' : '收起章节信息' }}</span>
                  <span class="toggle-icon">{{ isChapterInfoCollapsed ? '▼' : '▲' }}</span>
                </div>

                <div v-show="!isChapterInfoCollapsed">
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
                        <button @click="removePlot(index)" class="btn btn-error btn-xs remove-plot-btn">删除</button>
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
              </div>

              <!-- 章节内容 -->
              <div class="chapter-text" v-html="formatChapterContent(selectedChapter.chapterContent || '暂无章节内容')"></div>
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

  <!-- 章节重新生成弹窗 -->
  <div v-if="showRegenerateModal" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h3>重新生成本章内容</h3>
        <button class="close-btn" @click="cancelRegenerate">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>修改意见（必填）</label>
          <textarea
            v-model="regenerateInstruction"
            class="form-textarea"
            rows="5"
            placeholder="请描述你希望本章节在剧情、节奏、人物塑造、风格等方面如何调整，例如：加强悬念、减少无关日常、放大某个冲突等。"
          ></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="cancelRegenerate">取消</button>
        <button class="btn btn-primary" :disabled="aiDisabled" @click="confirmRegenerate">确认重新生成本章内容</button>
      </div>
    </div>
  </div>

  <!-- AI修订大纲弹窗 -->
  <div v-if="showRefineOutlineModal" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h3>AI修订故事大纲</h3>
        <button class="close-btn" @click="cancelRefineOutline">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>修改意见（必填）</label>
          <textarea
            v-model="refineOutlineInstruction"
            class="form-textarea"
            rows="5"
            placeholder="请输入你希望大纲如何调整，例如：加强冲突、明确主线目标、压缩日常、突出反派动机、增加三幕结构等。"
          ></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="cancelRefineOutline">取消</button>
        <button class="btn btn-primary" :disabled="aiDisabled || refiningOutline" @click="confirmRefineOutline">
          {{ refiningOutline ? '生成中...' : '确认生成新大纲' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 按需生成章节弹窗 -->
  <div v-if="showGenerateChaptersModal" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h3>按需生成章节</h3>
        <button class="close-btn" @click="cancelGenerateChapters">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>请选择要生成的章节数量</label>
          <div class="choice-grid">
            <button
              v-for="opt in chapterCountOptions"
              :key="opt.value"
              type="button"
              class="choice-btn"
              :class="{ active: selectedGenerateChaptersOption === opt.value }"
              @click="selectedGenerateChaptersOption = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="cancelGenerateChapters">取消</button>
        <button class="btn btn-primary" :disabled="aiDisabled || generatingChaptersBatch" @click="confirmGenerateChapters">
          {{ generatingChaptersBatch ? '启动中...' : '确认生成' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 按需生成章节内容弹窗 -->
  <div v-if="showGenerateChapterContentModal" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h3>按需生成章节内容</h3>
        <button class="close-btn" @click="cancelGenerateChapterContent">×</button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>请选择要生成的章节内容数量</label>
          <div class="helper-text">
            “全部”表示对<strong>已有章节</strong>中尚未生成内容的章节，全部生成内容。
          </div>
          <div class="choice-grid">
            <button
              v-for="opt in chapterCountOptions"
              :key="opt.value"
              type="button"
              class="choice-btn"
              :class="{ active: selectedGenerateChapterContentOption === opt.value }"
              @click="selectedGenerateChapterContentOption = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="cancelGenerateChapterContent">取消</button>
        <button class="btn btn-primary" :disabled="aiDisabled || generatingChapterContentBatch" @click="confirmGenerateChapterContent">
          {{ generatingChapterContentBatch ? '启动中...' : '确认生成' }}
        </button>
      </div>
    </div>
  </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { http } from '../lib/http/client';
import type { BaseResponse } from '../lib/types/base';
import { useAuthStore } from '../stores/auth';
import { useWalletStore } from '../stores/wallet';

interface ArticleRespDto {
  id: number;
  articleName: string;
  articleOutline: string;
  theme?: string;
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
  generationStatus?: number;
  plots?: PlotRespDto[];
}

const route = useRoute();
const router = useRouter();

const auth = useAuthStore();
const wallet = useWalletStore();
const { balance } = storeToRefs(wallet);
const { membershipActive } = storeToRefs(auth);
const aiDisabled = computed(
  () => !membershipActive.value || (balance.value?.availableBalanceCent ?? 0) <= 0
);

const article = ref<ArticleRespDto | null>(null);
const chapters = ref<ArticleChapterRespDto[]>([]);
const selectedChapterId = ref<number | null>(null);
const selectedChapter = ref<ArticleChapterRespDto | null>(null);

// 章节信息折叠状态：有内容时默认折叠，无内容时默认展开
const isChapterInfoCollapsed = ref(false);

// 编辑状态管理
const editingCorePlot = ref(false);
const editingWordCount = ref(false);
const editingPlots = ref(false);

// 生成状态跟踪
const generatingChapters = ref<Set<number>>(new Set());

// 操作按钮显示状态
const activeActionsChapter = ref<number | null>(null);

// 编辑表单数据
const editedCorePlot = ref('');
const editedWordCount = ref(0);
const editedPlots = ref<PlotRespDto[]>([]);

// 重新生成弹窗状态
const showRegenerateModal = ref(false);
const regenerateInstruction = ref('');
const regeneratingChapter = ref<ArticleChapterRespDto | null>(null);

// AI修订大纲弹窗状态
const showRefineOutlineModal = ref(false);
const refineOutlineInstruction = ref('');
const refiningOutline = ref(false);

type ChapterCountOptionValue = number | 'ALL';
const chapterCountOptions: Array<{ label: string; value: ChapterCountOptionValue }> = [
  { label: '1章', value: 1 },
  { label: '3章', value: 3 },
  { label: '5章', value: 5 },
  { label: '10章', value: 10 },
  { label: '30章', value: 30 },
  { label: '50章', value: 50 },
  { label: '100章', value: 100 },
  { label: '全部', value: 'ALL' },
];

// 按需生成章节弹窗状态
const showGenerateChaptersModal = ref(false);
const selectedGenerateChaptersOption = ref<ChapterCountOptionValue>('ALL');
const generatingChaptersBatch = ref(false);

// 按需生成章节内容弹窗状态
const showGenerateChapterContentModal = ref(false);
const selectedGenerateChapterContentOption = ref<ChapterCountOptionValue>('ALL');
const generatingChapterContentBatch = ref(false);


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

  // 根据章节是否已有内容设置章节信息折叠状态
  isChapterInfoCollapsed.value = !!chapter.chapterContent;

  // 重置编辑状态
  editingCorePlot.value = false;
  editingWordCount.value = false;
  editingPlots.value = false;
};

// 切换章节信息折叠/展开
const toggleChapterInfo = () => {
  isChapterInfoCollapsed.value = !isChapterInfoCollapsed.value;
};

// 显示操作按钮
const showActions = (chapter: ArticleChapterRespDto) => {
  activeActionsChapter.value = chapter.id;
};

// 隐藏操作按钮
const hideActions = () => {
  activeActionsChapter.value = null;
};


// 格式化章节内容
const formatChapterContent = (content: string) => {
  if (!content) return '暂无内容';
  // 将换行符转换为<br>标签
  return content.replace(/\n/g, '<br>');
};

// 生成章节内容
const generateChapterContent = async (chapter: ArticleChapterRespDto) => {
  if (generatingChapters.value.has(chapter.id)) return;

  generatingChapters.value.add(chapter.id);

  // 点击即提示任务已启动
  window.showNotification('章节内容生成任务已启动', 'success');

  try {
    const resp = await http.post<BaseResponse<boolean>>(
      `/api/articles/${article.value!.id}/generate-chapter-content/${chapter.id}`,
      {},
      { timeout: 180000, silentBizError: true } as any
    );
    const response = resp.data;
    if (response.code !== '00000000') {
      window.showNotification(
        response.msg || '章节内容生成失败，请稍后重试',
        'error'
      );
    }
  } catch (error: any) {
    console.error('生成章节内容失败：', error);
    const msg = error?.message || '章节内容生成失败，请稍后重试';
    window.showNotification(msg, 'error');
  } finally {
    generatingChapters.value.delete(chapter.id);
  }
};

// 打开章节内容重新生成弹窗
const openRegenerateDialog = (chapter: ArticleChapterRespDto) => {
  regeneratingChapter.value = chapter;
  regenerateInstruction.value = '';
  showRegenerateModal.value = true;
};

// 取消重新生成
const cancelRegenerate = () => {
  showRegenerateModal.value = false;
  regeneratingChapter.value = null;
  regenerateInstruction.value = '';
};

// 确认重新生成章节内容
const confirmRegenerate = async () => {
  if (!regeneratingChapter.value || !article.value) return;
  const instruction = regenerateInstruction.value.trim();
  if (!instruction) {
    window.showNotification('请先输入修改意见再重新生成章节内容', 'error');
    return;
  }

  try {
    window.showNotification('重新生成本章内容任务已启动', 'success');
    await http.post<BaseResponse<boolean>>(
      `/api/articles/${article.value.id}/generate-chapter-content/${regeneratingChapter.value.id}/regenerate`,
      { instruction },
      { timeout: 180000, silentBizError: true } as any
    );

    // 重新加载章节列表，并保持当前选中章节
    const currentArticleId = article.value.id;
    const currentChapterId = regeneratingChapter.value.id;
    try {
      const resp = await http.get<BaseResponse<ArticleChapterRespDto[]>>(
        `/api/articles/${currentArticleId}/chapters`
      );
      const response = resp.data;
      if (response.code === '00000000') {
        chapters.value = response.data || [];
        const updated = chapters.value.find(c => c.id === currentChapterId);
        if (updated) {
          selectChapter(updated);
        }
      }
    } catch (e) {
      console.error('重新加载章节列表失败:', e);
    }
  } catch (error) {
    console.error('重新生成章节内容失败：', error);
  } finally {
    showRegenerateModal.value = false;
    regeneratingChapter.value = null;
    regenerateInstruction.value = '';
  }
};

// 打开AI修订大纲弹窗
const openRefineOutlineDialog = () => {
  refineOutlineInstruction.value = '';
  showRefineOutlineModal.value = true;
};

const cancelRefineOutline = () => {
  showRefineOutlineModal.value = false;
  refineOutlineInstruction.value = '';
  refiningOutline.value = false;
};

const confirmRefineOutline = async () => {
  if (!article.value) return;
  const instruction = refineOutlineInstruction.value.trim();
  if (!instruction) {
    window.showNotification('请先输入修改意见', 'error');
    return;
  }

  refiningOutline.value = true;
  try {
    const resp = await http.post<BaseResponse<string>>(
      `/api/articles/${article.value.id}/refine-outline`,
      { instruction },
      { timeout: 180000, silentBizError: true } as any
    );
    const response = resp.data;
    if (response.code === '00000000') {
      if (article.value) {
        article.value.articleOutline = response.data || '';
      }
      window.showNotification('大纲已更新', 'success');
      showRefineOutlineModal.value = false;
      refineOutlineInstruction.value = '';
    } else {
      window.showNotification('生成失败：' + (response.msg || '未知错误'), 'error');
    }
  } catch (e: any) {
    console.error('AI修订大纲失败:', e);
    window.showNotification(e?.message || '生成失败，请稍后重试', 'error');
  } finally {
    refiningOutline.value = false;
  }
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

// 下载当前章节
const downloadCurrentChapter = () => {
  if (!article.value || !selectedChapter.value) return;

  const chapter = selectedChapter.value;
  const articleName = article.value.articleName || '未命名文章';
  const chapterNo = chapter.chapterNo ?? '';
  const chapterTitle = chapter.chapterTitle || '';
  const chapterContent = chapter.chapterContent || '';

  const contentLines = [
    `【${articleName}】`,
    chapterNo ? `第${chapterNo}章 ${chapterTitle}` : chapterTitle,
    '',
    chapterContent,
  ];

  const chapterText = contentLines.join('\n');
  const blob = new Blob([chapterText], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = chapterNo
    ? `${articleName}-第${chapterNo}章-${chapterTitle || '未命名章节'}.txt`
    : `${articleName}-${chapterTitle || '未命名章节'}.txt`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

// 整篇文章级别操作：生成章节 / 生成章节内容

const openGenerateChaptersDialog = () => {
  selectedGenerateChaptersOption.value = 'ALL';
  showGenerateChaptersModal.value = true;
};

const cancelGenerateChapters = () => {
  showGenerateChaptersModal.value = false;
  generatingChaptersBatch.value = false;
};

const confirmGenerateChapters = async () => {
  if (!article.value) return;
  if (generatingChaptersBatch.value) return;
  generatingChaptersBatch.value = true;

  const opt = selectedGenerateChaptersOption.value;
  const payload = opt === 'ALL' ? { all: true } : { count: opt };

  window.showNotification('章节生成任务已启动', 'success');
  // 点击确认后立即关闭弹窗（请求在后台继续）
  showGenerateChaptersModal.value = false;
  try {
    await http.post<BaseResponse<boolean>>(
      `/api/articles/${article.value.id}/generate-chapters`,
      payload,
      { timeout: 300000, silentBizError: true } as any
    );
  } catch (error) {
    console.error('章节生成失败:', error);
  } finally {
    generatingChaptersBatch.value = false;
  }
};

const openGenerateChapterContentDialog = () => {
  selectedGenerateChapterContentOption.value = 'ALL';
  showGenerateChapterContentModal.value = true;
};

const cancelGenerateChapterContent = () => {
  showGenerateChapterContentModal.value = false;
  generatingChapterContentBatch.value = false;
};

const confirmGenerateChapterContent = async () => {
  if (!article.value) return;
  if (generatingChapterContentBatch.value) return;
  generatingChapterContentBatch.value = true;

  const opt = selectedGenerateChapterContentOption.value;
  const payload = opt === 'ALL' ? { all: true } : { count: opt };

  window.showNotification('章节内容生成任务已启动', 'success');
  // 点击确认后立即关闭弹窗（请求在后台继续）
  showGenerateChapterContentModal.value = false;
  try {
    await http.post<BaseResponse<boolean>>(
      `/api/articles/${article.value.id}/generate-chapter-content`,
      payload,
      { timeout: 600000, silentBizError: true } as any
    );
  } catch (error) {
    console.error('章节内容生成失败:', error);
  } finally {
    generatingChapterContentBatch.value = false;
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


// 章节标题截断函数，确保至少显示10个字
const truncateTitle = (title: string, minLength: number = 10) => {
  if (!title) return '';

  // 如果标题长度小于等于最小显示长度，直接返回
  if (title.length <= minLength) {
    return title;
  }

  // 截取最小显示长度并添加省略号
  return title.substring(0, minLength) + '...';
};

onMounted(() => {
  const articleId = route.params.id as string;
  if (articleId) {
    fetchArticleDetail(articleId);
    fetchArticleChapters(articleId);
  }

  // 刷新会员与余额用于 AI 按钮禁用态
  auth.refreshSession().catch(() => {});
  wallet.refreshBalance().catch(() => {});
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

.outline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.outline-header h3 {
  margin: 0;
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

.content-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 2rem;
  min-height: 600px;
  max-width: 1200px;
  margin: 0 auto;
  padding-left: 0;
  padding-right: 1rem;
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
  margin-left: 1rem; /* 添加左边距保持美观 */
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
  justify-content: space-between;
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

.chapter-title-container {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.chapter-title {
  width: 100%;
  font-size: 0.875rem;
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.2s ease;
}


.chapter-actions {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 0.125rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
  padding: 0.125rem;
  border-radius: 0.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.delete-btn {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  padding: 0.125rem;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

.generate-btn {
  padding: 0.125rem 0.375rem;
  font-size: 0.625rem;
  background-color: var(--success, #10b981);
  color: white;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.generate-btn:hover:not(:disabled) {
  background-color: var(--success-dark, #059669);
}

.generate-btn:disabled {
  background-color: var(--gray, #9ca3af);
  cursor: not-allowed;
}

.delete-btn {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
  background-color: transparent;
  color: var(--gray, #6b7280);
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
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

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid var(--primary);
  padding-bottom: 0.5rem;
}

.content-header .chapter-title {
  font-size: 1.75rem;
  font-weight: bold;
  margin: 0;
  color: var(--text);
}

.content-header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* 本章下载按钮去掉鼠标悬停特效 */
.chapter-download-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  transition: none;
}

.chapter-download-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 章节信息区域 */
.chapter-info {
  background: var(--surface);
  border-radius: 0.75rem;
  padding: 1.5rem;
  margin-bottom: 2rem;
  border: 1px solid var(--border);
}

.chapter-info.collapsed {
  padding-bottom: 0.75rem;
}

.chapter-info-toggle {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  margin-bottom: 0.5rem;
}

.chapter-info-toggle:hover {
  color: var(--primary);
}

.toggle-icon {
  font-size: 0.75rem;
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

/* 按需选择按钮 */
.choice-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.5rem;
}

.choice-btn {
  border: 1px solid var(--border-light, #e5e7eb);
  background: white;
  border-radius: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  font-size: 0.95rem;
  transition: all 0.15s ease;
}

.choice-btn:hover {
  border-color: var(--primary, #2563eb);
  background: rgba(37, 99, 235, 0.06);
}

.choice-btn.active {
  border-color: var(--primary, #2563eb);
  background: rgba(37, 99, 235, 0.10);
  color: var(--primary, #2563eb);
  font-weight: 600;
}

.helper-text {
  font-size: 0.9rem;
  color: var(--text-secondary, #6b7280);
  margin-top: 0.25rem;
  margin-bottom: 0.75rem;
  line-height: 1.4;
}

@media (max-width: 640px) {
  .choice-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

</style>