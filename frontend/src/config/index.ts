// 前端配置文件
// 主要配置已在 vite.config.ts 中通过全局变量定义

// 导出vite全局配置的类型定义
declare global {
  interface Window {
    __API_CONFIG__: typeof API_CONFIG;
    __APP_CONFIG__: typeof APP_CONFIG;
    __THEME_CONFIG__: typeof THEME_CONFIG;
    __PAGINATION_CONFIG__: typeof PAGINATION_CONFIG;
  }
}

// 从全局变量获取配置，如果没有则使用默认值
export const API_CONFIG = (window as any).__API_CONFIG__ || {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  TIMEOUT: 10000,
  RETRY_TIMES: 3,
};

export const APP_CONFIG = (window as any).__APP_CONFIG__ || {
  NAME: 'AI Creation',
  VERSION: '1.0.0',
  DESCRIPTION: 'AI Creation Platform',
};

export const THEME_CONFIG = (window as any).__THEME_CONFIG__ || {
  PRIMARY_COLOR: '#1890ff',
  SUCCESS_COLOR: '#52c41a',
  WARNING_COLOR: '#faad14',
  ERROR_COLOR: '#f5222d',
};

export const PAGINATION_CONFIG = (window as any).__PAGINATION_CONFIG__ || {
  DEFAULT_PAGE_SIZE: 10,
  PAGE_SIZE_OPTIONS: [10, 20, 50, 100],
};
