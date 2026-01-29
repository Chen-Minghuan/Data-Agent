import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import { HttpStatusCode, ErrorCode } from '../constants/errorCode';
import type { TokenPairResponse } from '../types/auth';

const http = axios.create({
    baseURL: '/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

http.interceptors.request.use(
    (config) => {
        const { accessToken } = useAuthStore.getState();
        if (accessToken) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

http.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config as any;

        if (
            error.response?.status === HttpStatusCode.UNAUTHORIZED &&
            error.response?.data?.code === ErrorCode.NOT_LOGIN_ERROR &&
            !originalRequest._retry
        ) {
            originalRequest._retry = true;

            try {
                const { refreshToken, user, setAuth } = useAuthStore.getState();
                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                const refreshResponse = await http.post<TokenPairResponse>('/auth/refresh', { refreshToken });
                const { accessToken, refreshToken: newRefreshToken } = refreshResponse.data;

                setAuth(user, accessToken, newRefreshToken);

                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return http(originalRequest);
            } catch (refreshError) {
                const { clearAuth, openLoginModal } = useAuthStore.getState();
                clearAuth();
                openLoginModal();
                return Promise.reject(refreshError);
            }
        }

        if (error.response?.data?.code === ErrorCode.NOT_LOGIN_ERROR) {
            const { openLoginModal } = useAuthStore.getState();
            openLoginModal();
        }

        return Promise.reject(error);
    }
);

export default http;
