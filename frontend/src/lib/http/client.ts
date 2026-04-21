import axios, { AxiosError, AxiosHeaders } from 'axios';
import { mapErrorCodeToMessage } from '../i18n/error-map';
import { API_CONFIG } from '../../config';

const SUCCESS_CODE = '00000000';

export const http = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 将 localStorage 中的 userId 同步到 axios 默认头，保证每条请求都带 X-User-Id（仅靠拦截器在部分环境下仍可能丢失）
 */
export function syncHttpUserIdHeader(): void {
  if (typeof localStorage === 'undefined') return;
  const uid = localStorage.getItem('userId');
  const defaults = http.defaults.headers as any;
  const normalizedUserId = uid && /^\d+$/.test(uid.trim()) ? uid.trim() : null;

  const setHeader = (holder: any, value: string | null) => {
    if (!holder) return;
    if (typeof holder.set === 'function') {
      if (value) {
        holder.set('X-User-Id', value);
      } else if (typeof holder.delete === 'function') {
        holder.delete('X-User-Id');
      } else {
        delete holder['X-User-Id'];
      }
      return;
    }
    if (value) {
      holder['X-User-Id'] = value;
    } else {
      delete holder['X-User-Id'];
    }
  };

  // Axios 在不同版本/构建形态下 defaults.headers 可能是 AxiosHeaders 或普通对象
  setHeader(defaults, normalizedUserId);
  setHeader(defaults?.common, normalizedUserId);
}

syncHttpUserIdHeader();

function isLoginRequest(url?: string): boolean {
  if (!url) return false;
  return url.includes('/users/login');
}

http.interceptors.request.use((config) => {
  // Axios 1.x 使用 AxiosHeaders，用 .set 才能稳定带上自定义头
  const headers = AxiosHeaders.from(config.headers ?? {});

  const token = localStorage.getItem('token');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // 优先携带当前用户ID；缺失时交由后端 Session 兜底解析
  if (!isLoginRequest(config.url)) {
    const rawUserId = localStorage.getItem('userId');
    if (rawUserId && /^\d+$/.test(rawUserId.trim())) {
      headers.set('X-User-Id', rawUserId.trim());
    } else {
      headers.delete('X-User-Id');
    }
  }

  config.headers = headers;
  return config;
});

http.interceptors.response.use(
  (resp) => {
    const silentBizError = Boolean((resp.config as any)?.silentBizError);
    // 兼容后端“HTTP 200 + 业务码失败”的返回风格（如参数校验失败 code=4001）
    const data: any = resp?.data;
    if (data && typeof data === 'object' && 'code' in data) {
      const code = String(data.code ?? '');
      if (code && code !== SUCCESS_CODE) {
        const traceId = resp.headers?.['x-trace-id'] as string | undefined;
        const mapped = mapErrorCodeToMessage(code);
        const message = data.msg || mapped || '请求失败';

        if (!silentBizError) {
          // 尽量给用户即时提示（如果页面没有显式 catch 并提示，也不至于“静默失败”）
          try {
            (window as any)?.showNotification?.(message, 'error');
          } catch {
            // ignore
          }

          return Promise.reject({ code, message, traceId, raw: resp, biz: true });
        }
      }
    }
    return resp;
  },
  (error: AxiosError) => {
    const data = error.response?.data as { code?: string; msg?: string } | undefined;
    const code = data?.code as string | undefined;
    const traceId = error.response?.headers?.['x-trace-id'] as string | undefined;
    const serverMsg = data?.msg != null && String(data.msg).trim() !== '' ? String(data.msg).trim() : '';
    const message =
      serverMsg || (code ? mapErrorCodeToMessage(code) : '') || error.message || '请求失败';
    return Promise.reject({ code, message, traceId, raw: error });
  }
);


