<template>
  <div>
    <div class="card mb-6">
      <div class="card-header">
        <h2 class="text-xl font-semibold text-text">文章生成配置管理</h2>
      </div>

      <div class="card-body">
        <div class="toolbar">
          <div class="search-section">
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="请输入主题搜索..."
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
              新增文章生成配置
            </button>
          </div>
        </div>

        <div class="table-container">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">加载中...</p>
          </div>

          <table v-else-if="articleGenerationConfigs.length > 0" class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>主题</th>
                <th>性别</th>
                <th>题材</th>
                <th>情节</th>
                <th>角色</th>
                <th>风格</th>
                <th>附加特点</th>
                <th>总字数预估</th>
                <th>每章字数预估</th>
                <th>待生成数量</th>
                <th>创建时间</th>
                <th class="action-column">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="config in articleGenerationConfigs" :key="config.id">
                <td>{{ config.id }}</td>
                <td>{{ config.theme }}</td>
                <td>{{ config.gender || '-' }}</td>
                <td>{{ config.genre || '-' }}</td>
                <td>{{ config.plot || '-' }}</td>
                <td>{{ config.characterType || '-' }}</td>
                <td>{{ config.style || '-' }}</td>
                <td>
                  <div class="characteristics-tags">
                    <span
                      v-for="char in config.additionalCharacteristics?.split(',') || []"
                      :key="char"
                      class="tag"
                    >
                      {{ char }}
                    </span>
                  </div>
                </td>
                <td>{{ config.totalWordCountEstimate || 100000 }}</td>
                <td>{{ config.chapterWordCountEstimate || 5000 }}</td>
                <td>{{ config.pendingCount }}</td>
                <td>{{ formatDate(config.createTime) }}</td>
                <td class="action-column">
                  <div class="flex gap-2">
                    <button @click="editConfig(config)" class="btn btn-outline btn-sm">
                      编辑
                    </button>
                    <select @change="handleActionSelect($event, config)" class="action-select">
                      <option value="">更多操作</option>
                      <option :value="'generateTitle_' + config.id">
                        生成标题
                      </option>
                      <option
                        :value="'delete_' + config.id"
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
            <p class="empty-text">暂无文章生成配置数据</p>
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

    <!-- 新增文章生成配置模态框 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="showCreateModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">新增文章生成配置</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="createConfig" class="space-y-4">
            <div class="form-group">
              <label class="form-label">主题 *</label>
              <input v-model="newConfig.theme" class="form-input" placeholder="例如：程序员逆袭" required />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">性别分类</label>
                <select v-model="newConfig.gender" class="form-select">
                  <option value="">请选择性别</option>
                  <option v-for="option in genderOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">题材分类</label>
                <select v-model="newConfig.genre" class="form-select">
                  <option value="">请选择题材</option>
                  <option v-for="option in genreOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">情节分类</label>
                <select v-model="newConfig.plot" class="form-select">
                  <option value="">请选择情节</option>
                  <option v-for="option in plotOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">角色分类</label>
                <select v-model="newConfig.characterType" class="form-select">
                  <option value="">请选择角色</option>
                  <option v-for="option in characterTypeOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">风格分类</label>
              <select v-model="newConfig.style" class="form-select">
                <option value="">请选择风格</option>
                <option v-for="option in styleOptions" :key="option.dictValue" :value="option.dictValue">
                  {{ option.dictValue }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">附加特点</label>
              <div class="characteristics-input">
                <input
                  v-model="newConfig.additionalCharacteristics"
                  class="form-input"
                  placeholder="请输入附加特点，用逗号分隔"
                />
                <div class="characteristics-buttons">
                  <button
                    v-for="char in availableCharacteristics"
                    :key="char.dictValue"
                    type="button"
                    :class="['char-btn', { active: isCharacteristicSelected(char.dictValue, newConfig.additionalCharacteristics) }]"
                    @click="toggleCharacteristic(char.dictValue, newConfig)"
                  >
                    {{ char.dictValue }}
                  </button>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">总字数预估</label>
                <input v-model.number="newConfig.totalWordCountEstimate" type="number" class="form-input" min="1000" placeholder="100000" />
                <small class="form-hint">默认 100000 字</small>
              </div>
              <div class="form-group">
                <label class="form-label">每章节字数预估</label>
                <input v-model.number="newConfig.chapterWordCountEstimate" type="number" class="form-input" min="500" placeholder="5000" />
                <small class="form-hint">默认 5000 字</small>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">待生成数量</label>
              <input v-model.number="newConfig.pendingCount" type="number" class="form-input" min="0" />
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

    <!-- 编辑文章生成配置模态框 -->
    <div v-if="showEditModal && editingConfig" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content card" @click.stop>
        <div class="card-header">
          <h3 class="text-lg font-semibold">编辑文章生成配置</h3>
        </div>
        <div class="card-body">
          <form @submit.prevent="updateConfig" class="space-y-4">
            <div class="form-group">
              <label class="form-label">主题 *</label>
              <input v-model="editingConfig.theme" class="form-input" placeholder="例如：程序员逆袭" required />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">性别分类</label>
                <select v-model="editingConfig.gender" class="form-select">
                  <option value="">请选择性别</option>
                  <option v-for="option in genderOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">题材分类</label>
                <select v-model="editingConfig.genre" class="form-select">
                  <option value="">请选择题材</option>
                  <option v-for="option in genreOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">情节分类</label>
                <select v-model="editingConfig.plot" class="form-select">
                  <option value="">请选择情节</option>
                  <option v-for="option in plotOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">角色分类</label>
                <select v-model="editingConfig.characterType" class="form-select">
                  <option value="">请选择角色</option>
                  <option v-for="option in characterTypeOptions" :key="option.dictValue" :value="option.dictValue">
                    {{ option.dictValue }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">风格分类</label>
              <select v-model="editingConfig.style" class="form-select">
                <option value="">请选择风格</option>
                <option v-for="option in styleOptions" :key="option.dictValue" :value="option.dictValue">
                  {{ option.dictValue }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">附加特点</label>
              <div class="characteristics-input">
                <input
                  v-model="editingConfig.additionalCharacteristics"
                  class="form-input"
                  placeholder="请输入附加特点，用逗号分隔"
                />
                <div class="characteristics-buttons">
                  <button
                    v-for="char in availableCharacteristics"
                    :key="char.dictValue"
                    type="button"
                    :class="['char-btn', { active: isCharacteristicSelected(char.dictValue, editingConfig.additionalCharacteristics) }]"
                    @click="toggleCharacteristic(char.dictValue, editingConfig)"
                  >
                    {{ char.dictValue }}
                  </button>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">总字数预估</label>
                <input v-model.number="editingConfig.totalWordCountEstimate" type="number" class="form-input" min="1000" placeholder="100000" />
                <small class="form-hint">默认 100000 字</small>
              </div>
              <div class="form-group">
                <label class="form-label">每章节字数预估</label>
                <input v-model.number="editingConfig.chapterWordCountEstimate" type="number" class="form-input" min="500" placeholder="5000" />
                <small class="form-hint">默认 5000 字</small>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">待生成数量</label>
              <input v-model.number="editingConfig.pendingCount" type="number" class="form-input" min="0" />
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
import type { BaseResponse, PageRespDto } from '../lib/types/base';

interface ArticleGenerationConfigListRespDto {
  id: number;
  theme: string;
  gender?: string;
  genre?: string;
  plot?: string;
  characterType?: string;
  style?: string;
  additionalCharacteristics?: string;
  totalWordCountEstimate?: number;
  chapterWordCountEstimate?: number;
  pendingCount: number;
  createTime: string;
}

interface ArticleGenerationConfigCreateReqDto {
  theme: string;
  gender?: string;
  genre?: string;
  plot?: string;
  characterType?: string;
  style?: string;
  additionalCharacteristics?: string;
  totalWordCountEstimate?: number;
  chapterWordCountEstimate?: number;
  pendingCount?: number;
}

interface ArticleGenerationConfigUpdateReqDto {
  id: number;
  theme: string;
  gender?: string;
  genre?: string;
  plot?: string;
  characterType?: string;
  style?: string;
  additionalCharacteristics?: string;
  totalWordCountEstimate?: number;
  chapterWordCountEstimate?: number;
  pendingCount?: number;
}

interface ArticleGenerationConfigDeleteReqDto {
  id: number;
}

interface DictionaryRespDto {
  id: number;
  dictKey: string;
  dictValue: string;
  sortOrder: number;
  createTime: string;
  updateTime: string;
}

const articleGenerationConfigs = ref<ArticleGenerationConfigListRespDto[]>([]);
const availableCharacteristics = ref<DictionaryRespDto[]>([]);
const genderOptions = ref<DictionaryRespDto[]>([]);
const genreOptions = ref<DictionaryRespDto[]>([]);
const plotOptions = ref<DictionaryRespDto[]>([]);
const characterTypeOptions = ref<DictionaryRespDto[]>([]);
const styleOptions = ref<DictionaryRespDto[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const searchKeyword = ref('');
const generatingTitle = ref(false);
const generatingConfigId = ref<number | null>(null);

// 下拉菜单状态
const openDropdownId = ref<number | null>(null);

const showCreateModal = ref(false);
const showEditModal = ref(false);
const editingConfig = ref<ArticleGenerationConfigListRespDto | null>(null);

const newConfig = ref<ArticleGenerationConfigCreateReqDto>({
  theme: '',
  gender: '',
  genre: '',
  plot: '',
  characterType: '',
  style: '',
  additionalCharacteristics: '',
  totalWordCountEstimate: 100000,
  chapterWordCountEstimate: 5000,
  pendingCount: 0
});

const totalPages = computed(() => Math.ceil(total.value / pageSize.value));

// 加载文章生成配置列表
const loadConfigs = async () => {
  loading.value = true;
  try {
    const resp = await http.post<BaseResponse<PageRespDto<ArticleGenerationConfigListRespDto>>>(
      '/api/article-generation-configs/list',
      {
        pageNo: currentPage.value,
        pageSize: pageSize.value,
        theme: searchKeyword.value || undefined
      }
    );

    const response = resp.data;
    if (response.code === '00000000') {
      const page = response.data;
      articleGenerationConfigs.value = page?.list || [];
      total.value = page?.total || 0;
    } else {
      console.error('加载文章生成配置失败:', response.msg);
    }
  } catch (error) {
    console.error('加载文章生成配置失败:', error);
  } finally {
    loading.value = false;
  }
};

// 加载字典选项
const loadDictionaryOptions = async () => {
  const keys = ['文章特点', '性别分类', '题材分类', '情节分类', '角色分类', '风格分类'];

  for (const key of keys) {
    try {
      const resp = await http.get<BaseResponse<DictionaryRespDto[]>>(`/api/dictionaries/key/${encodeURIComponent(key)}`);
      const response = resp.data;
      if (response.code === '00000000') {
        // 去重处理，避免同一特点出现多次
        const uniqueData = response.data.filter((item, index, self) =>
          index === self.findIndex(t => t.dictValue === item.dictValue)
        );

        switch (key) {
          case '文章特点':
            availableCharacteristics.value = uniqueData;
            break;
          case '性别分类':
            genderOptions.value = uniqueData;
            break;
          case '题材分类':
            genreOptions.value = uniqueData;
            break;
          case '情节分类':
            plotOptions.value = uniqueData;
            break;
          case '角色分类':
            characterTypeOptions.value = uniqueData;
            break;
          case '风格分类':
            styleOptions.value = uniqueData;
            break;
        }
      } else {
        console.error(`加载${key}失败:`, response.msg);
      }
    } catch (error) {
      console.error(`加载${key}失败:`, error);
    }
  }
};

// 创建文章生成配置
const createConfig = async () => {
  try {
    // 确保无论用户是否填写，这两个字段都有默认值传给后端
    const payload: ArticleGenerationConfigCreateReqDto = {
      ...newConfig.value,
      totalWordCountEstimate: newConfig.value.totalWordCountEstimate ?? 100000,
      chapterWordCountEstimate: newConfig.value.chapterWordCountEstimate ?? 5000
    };

    const resp = await http.post<BaseResponse<number>>('/api/article-generation-configs', payload);
    const response = resp.data;
    if (response.code === '00000000') {
      showCreateModal.value = false;
      newConfig.value = {
        theme: '',
        gender: '',
        genre: '',
        plot: '',
        characterType: '',
        style: '',
        additionalCharacteristics: '',
        totalWordCountEstimate: 100000,
        chapterWordCountEstimate: 5000,
        pendingCount: 0
      };
      await loadConfigs();
    } else {
      window.showNotification('创建失败: ' + response.msg, 'error');
    }
  } catch (error) {
    console.error('创建文章生成配置失败:', error);
    window.showNotification('创建失败，请查看控制台日志', 'error');
  }
};

// 编辑文章生成配置
const editConfig = (config: ArticleGenerationConfigListRespDto) => {
  // 确保编辑时总字数和每章字数都有默认值（即使后端是 null）
  editingConfig.value = {
    ...config,
    totalWordCountEstimate: config.totalWordCountEstimate ?? 100000,
    chapterWordCountEstimate: config.chapterWordCountEstimate ?? 5000
  };
  showEditModal.value = true;
};

// 更新文章生成配置
const updateConfig = async () => {
  if (!editingConfig.value) return;

  try {
    const updateData: ArticleGenerationConfigUpdateReqDto = {
      id: editingConfig.value.id,
      theme: editingConfig.value.theme,
      gender: editingConfig.value.gender,
      genre: editingConfig.value.genre,
      plot: editingConfig.value.plot,
      characterType: editingConfig.value.characterType,
      style: editingConfig.value.style,
      additionalCharacteristics: editingConfig.value.additionalCharacteristics,
      totalWordCountEstimate: editingConfig.value.totalWordCountEstimate,
      chapterWordCountEstimate: editingConfig.value.chapterWordCountEstimate,
      pendingCount: editingConfig.value.pendingCount
    };

    const resp = await http.put<BaseResponse<boolean>>('/api/article-generation-configs', updateData);
    const response = resp.data;
    if (response.code === '00000000') {
      showEditModal.value = false;
      editingConfig.value = null;
      await loadConfigs();
    } else {
      window.showNotification('更新失败: ' + response.msg, 'error');
    }
  } catch (error) {
    console.error('更新文章生成配置失败:', error);
    window.showNotification('更新失败，请查看控制台日志', 'error');
  }
};

// 删除文章生成配置
const deleteConfig = async (config: ArticleGenerationConfigListRespDto) => {

  try {
    const deleteData: ArticleGenerationConfigDeleteReqDto = {
      id: config.id
    };

    const resp = await http.delete<BaseResponse<boolean>>('/api/article-generation-configs', { data: deleteData });
    const response = resp.data;
    if (response.code === '00000000') {
      await loadConfigs();
    } else {
      window.showNotification('删除失败: ' + response.msg, 'error');
    }
  } catch (error) {
    console.error('删除文章生成配置失败:', error);
    window.showNotification('删除失败，请查看控制台日志', 'error');
  }
};

// 切换文章特点选择
const toggleCharacteristic = (characteristic: string, target: any) => {
  const currentChars = target.additionalCharacteristics ? target.additionalCharacteristics.split(',').map((s: string) => s.trim()) : [];
  const index = currentChars.indexOf(characteristic);

  if (index > -1) {
    // 移除
    currentChars.splice(index, 1);
  } else {
    // 添加
    currentChars.push(characteristic);
  }

  target.additionalCharacteristics = currentChars.join(',');
};

// 检查特点是否已选择
const isCharacteristicSelected = (characteristic: string, characteristicsStr?: string): boolean => {
  if (!characteristicsStr) return false;
  const currentChars = characteristicsStr.split(',').map(s => s.trim());
  return currentChars.includes(characteristic);
};

// 搜索
const handleSearch = () => {
  currentPage.value = 1;
  loadConfigs();
};

// 下拉菜单操作
const toggleDropdown = (configId: number) => {
  if (openDropdownId.value === configId) {
    openDropdownId.value = null;
  } else {
    openDropdownId.value = configId;
  }
};

const handleDropdownAction = (action: () => void, configId: number) => {
  action();
  openDropdownId.value = null; // 执行操作后关闭下拉菜单
};

// 处理select选择动作
const handleActionSelect = (event: Event, config: ArticleGenerationConfigListRespDto) => {
  const target = event.target as HTMLSelectElement;
  const value = target.value;

  if (!value) return; // 如果选择的是"更多操作"，不执行任何操作

  if (value.startsWith('generateTitle_')) {
    generateTitle(config);
  } else if (value.startsWith('delete_')) {
    deleteConfig(config);
  }

  // 重置select为默认值
  target.value = '';
};

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = '';
  currentPage.value = 1;
  loadConfigs();
};

// 生成标题
const generateTitle = async (config: ArticleGenerationConfigListRespDto) => {
  if (confirm(`确定要为配置"${config.theme}"生成一个新的文章标题吗？`)) {
    generatingTitle.value = true;
    generatingConfigId.value = config.id;

    // 全局改为异步调用：点击即提示成功，不等待后端
    window.showNotification('文章标题生成任务已启动', 'success');
    http.post<BaseResponse<number>>(
      `/api/articles/generate-title/${config.id}`,
      {},
      { timeout: 60000, silentBizError: true } as any
    ).catch((error) => {
      console.error('生成标题失败:', error);
    }).finally(() => {
      generatingTitle.value = false;
      generatingConfigId.value = null;
    });
  }
};

// 改变页码
const changePage = (page: number) => {
  currentPage.value = page;
  loadConfigs();
};

// 改变每页大小
const handlePageSizeChange = () => {
  currentPage.value = 1;
  loadConfigs();
};

// 格式化日期
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN');
};

// 组件挂载时加载数据
onMounted(() => {
  loadConfigs();
  loadDictionaryOptions();
});
</script>

<style scoped>
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

/* 文章特点输入区域 */
.characteristics-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.characteristics-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.char-btn {
  padding: 0.25rem 0.75rem;
  border: 1px solid var(--border);
  background-color: white;
  color: var(--text);
  border-radius: 0.375rem;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.char-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.char-btn.active {
  background-color: var(--primary);
  color: white;
  border-color: var(--primary);
}

/* 基础样式 */
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
  margin-bottom: 1.5rem;
  gap: 1rem;
}

.search-section {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.search-input {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  min-width: 250px;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}


.table-container {
  margin-bottom: 1.5rem;
}


.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 2px solid var(--border);
  border-top: 2px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  margin-top: 1rem;
  color: var(--text-secondary);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.empty-text {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
}

.pagination-info {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-item {
  padding: 0.5rem 1rem;
  border: 1px solid var(--border);
  background-color: white;
  color: var(--text);
  border-radius: 0.375rem;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.875rem;
}

.pagination-item:hover:not(:disabled) {
  background-color: var(--primary);
  color: white;
  border-color: var(--primary);
}

.pagination-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: var(--text-secondary);
}

.pagination-current {
  padding: 0.5rem 1rem;
  background-color: var(--primary);
  color: white;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.page-size-select {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-size-select select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-size-select select:hover {
  border-color: var(--primary);
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
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
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
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  resize: vertical;
  transition: all 0.2s ease;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background-color: white;
  color: var(--text);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.form-select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 0.5rem;
  }
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


@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
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

.action-column {
  width: 140px;
}

.table-container {
  overflow-x: auto;
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

.text-error {
  color: var(--error);
}

.text-text {
  color: var(--text);
}

.text-text-secondary {
  color: var(--text-secondary);
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

.ml-4 {
  margin-left: 1rem;
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

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.mr-1 {
  margin-right: 0.25rem;
}

.animate-spin {
  animation: spin 1s linear infinite;
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

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    min-width: auto;
  }

  .pagination {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .pagination-controls {
    justify-content: center;
  }

  .characteristics-buttons {
    justify-content: center;
  }
}
</style>