import axios, { AxiosError } from 'axios';
import { mapErrorCodeToMessage } from '../i18n/error-map';
import { API_CONFIG } from '../../config';

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
  (resp) => resp,
  (error: AxiosError) => {
    const code = (error.response?.data as any)?.code as string | undefined;
    const traceId = error.response?.headers?.['x-trace-id'] as string | undefined;
    const message = code ? mapErrorCodeToMessage(code) : error.message;
    // 可在此统一上报埋点/日志，关联 traceId
    return Promise.reject({ code, message, traceId, raw: error });
  }
);


