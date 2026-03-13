import axios, { AxiosError } from 'axios';
import { mapErrorCodeToMessage } from '../i18n/error-map';
import { API_CONFIG } from '../../config';

const SUCCESS_CODE = '00000000';

export const http = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers ?? {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
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
    const code = (error.response?.data as any)?.code as string | undefined;
    const traceId = error.response?.headers?.['x-trace-id'] as string | undefined;
    const message = code ? mapErrorCodeToMessage(code) : error.message;
    // 可在此统一上报埋点/日志，关联 traceId
    return Promise.reject({ code, message, traceId, raw: error });
  }
);


