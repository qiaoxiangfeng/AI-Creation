import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 全局配置常量
const APP_CONFIG = {
  NAME: 'AI Creation',
  VERSION: '1.0.0',
  DESCRIPTION: 'AI Creation Platform',
} as const;

// 开发服务器配置
const DEV_SERVER_CONFIG = {
  PORT: 5173,
  HOST: 'localhost',
  PROXY: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      ws: true,
    },
  },
} as const;

// API配置
const API_CONFIG = {
  BASE_URL: 'http://localhost:8080',
  TIMEOUT: 10000,
  RETRY_TIMES: 3,
} as const;

// 主题配置
const THEME_CONFIG = {
  PRIMARY_COLOR: '#1890ff',
  SUCCESS_COLOR: '#52c41a',
  WARNING_COLOR: '#faad14',
  ERROR_COLOR: '#f5222d',
} as const;

// 分页配置
const PAGINATION_CONFIG = {
  DEFAULT_PAGE_SIZE: 10,
  PAGE_SIZE_OPTIONS: [10, 20, 50, 100],
} as const;

export default defineConfig({
  plugins: [vue()],
  
  // 路径解析配置
  resolve: {
    alias: {
      '@': './src',
    },
  },
  
  // 开发服务器配置
  server: {
    port: DEV_SERVER_CONFIG.PORT,
    host: DEV_SERVER_CONFIG.HOST,
    proxy: DEV_SERVER_CONFIG.PROXY,
    open: false, // 不自动打开浏览器，由启动脚本控制
  },
  
  // 构建配置
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          utils: ['axios'],
        },
      },
    },
  },
  
  // 环境变量定义
  define: {
    __VUE_OPTIONS_API__: true,
    __VUE_PROD_DEVTOOLS__: false,
    __APP_CONFIG__: JSON.stringify(APP_CONFIG),
    __API_CONFIG__: JSON.stringify(API_CONFIG),
    __THEME_CONFIG__: JSON.stringify(THEME_CONFIG),
    __PAGINATION_CONFIG__: JSON.stringify(PAGINATION_CONFIG),
  },
  
  // 优化配置
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios'],
  },
  
  // 预览配置
  preview: {
    port: 4173,
    host: true,
  },
});


