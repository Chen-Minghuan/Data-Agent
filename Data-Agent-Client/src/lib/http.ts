import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import { triggerLoginModal } from '../store/authStore';
import { authService } from '../services/auth.service';
import { HttpStatusCode, ErrorCode } from '../constants/errorCode';

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
        const originalRequest = error.config;

        if (
            error.response?.status === HttpStatusCode.UNAUTHORIZED &&
            error.response?.data?.code === ErrorCode.NOT_LOGIN_ERROR &&
            !originalRequest._retry
        ) {
            originalRequest._retry = true;

            try {
                const refreshToken = useAuthStore.getState().refreshToken;
                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                const { accessToken, refreshToken: newRefreshToken } = await authService.refresh(refreshToken);

                useAuthStore.getState().setAuth(
                    useAuthStore.getState().user,
                    accessToken,
                    newRefreshToken
                );

                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return http(originalRequest);
            } catch (refreshError) {
                useAuthStore.getState().clearAuth();
                triggerLoginModal();
                return Promise.reject(refreshError);
            }
        }

        if (error.response?.data?.code === ErrorCode.NOT_LOGIN_ERROR) {
            triggerLoginModal();
        }

        return Promise.reject(error);
    }
);

export default http;
