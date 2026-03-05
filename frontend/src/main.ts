import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './assets/main.css';

// 全局通知函数
declare global {
  interface Window {
    showNotification: (message: string, type?: 'success' | 'error' | 'warning' | 'info', duration?: number) => void;
  }
}

// 创建通知容器
let notificationContainer: HTMLDivElement | null = null;

const createNotificationContainer = () => {
  if (!notificationContainer) {
    notificationContainer = document.createElement('div');
    notificationContainer.className = 'notification-container';
    document.body.appendChild(notificationContainer);
  }
  return notificationContainer;
};

const showNotification = (message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info', duration: number = 2000) => {
  const container = createNotificationContainer();

  const notification = document.createElement('div');
  notification.className = `notification notification-${type}`;

  const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : type === 'warning' ? '⚠' : 'ℹ';

  notification.innerHTML = `
    <div class="notification-icon">${icon}</div>
    <div class="notification-content">${message}</div>
    <button class="notification-close">×</button>
  `;

  container.appendChild(notification);

  // 关闭按钮事件
  const closeBtn = notification.querySelector('.notification-close') as HTMLButtonElement;
  closeBtn.addEventListener('click', () => {
    removeNotification(notification);
  });

  // 显示通知
  setTimeout(() => {
    notification.classList.add('show');
  }, 10);

  // 自动隐藏
  const timeoutId = setTimeout(() => {
    removeNotification(notification);
  }, duration);

  const removeNotification = (element: HTMLElement) => {
    element.classList.add('hide');
    setTimeout(() => {
      if (element.parentNode) {
        element.parentNode.removeChild(element);
      }
    }, 300);
  };

  return {
    remove: () => {
      clearTimeout(timeoutId);
      removeNotification(notification);
    }
  };
};

// 将通知函数添加到全局
window.showNotification = showNotification;

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount('#app');


