import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import toast from 'react-hot-toast';
import { useAuthStore } from '@/shared/store/auth';

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Request Interceptor ──
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['X-Client-Type'] = 'web';
  return config;
});

// ── Response Interceptor (Refresh Token 재발급 포함) ──
let isRefreshing = false;
let failedQueue: {
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}[] = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((p) => {
    if (token) {
      p.resolve(token);
    } else {
      p.reject(error);
    }
  });
  failedQueue = [];
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;
    const errorData = error.response?.data as
      | { error?: { code?: string; message?: string } }
      | undefined;

    // 401 + 아직 재시도하지 않은 요청 → Refresh Token으로 갱신 시도
    if (status === 401 && !originalRequest._retry) {
      // 로그인·회원가입 요청은 refresh 시도하지 않음
      const url = originalRequest.url || '';
      if (url.includes('/applicant/login') || url.includes('/applicant/join')) {
        toast.error(errorData?.error?.message || '이메일 또는 비밀번호가 일치하지 않습니다.');
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // WEB: Refresh Token은 HttpOnly 쿠키로 자동 전송
        const { data } = await axios.post('/api/applicant/refresh', null, {
          withCredentials: true,
        });

        const newToken: string = data.data.accessToken;
        const currentUser = useAuthStore.getState().user;
        if (currentUser) {
          useAuthStore.getState().setAuth(newToken, currentUser);
        }
        processQueue(null, newToken);

        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        useAuthStore.getState().logout();
        window.location.href = '/login';
        toast.error('인증이 만료되었습니다. 다시 로그인해주세요.');
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // 그 외 에러 처리
    switch (status) {
      case 400:
        toast.error(errorData?.error?.message || '잘못된 요청입니다.');
        break;
      case 403:
        toast.error(errorData?.error?.message || '접근 권한이 없습니다.');
        break;
      case 404:
        toast.error(errorData?.error?.message || '요청한 리소스를 찾을 수 없습니다.');
        break;
      case 409:
        toast.error(errorData?.error?.message || '중복된 데이터가 존재합니다.');
        break;
      default:
        if (status && status >= 500) {
          toast.error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        }
        break;
    }

    return Promise.reject(error);
  },
);

export { apiClient };
