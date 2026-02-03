import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User } from '../types/auth';
import { decodeJwt } from '../lib/jwtUtil';

// 定义一个全局事件类型
declare global {
    interface Window {
        __OPEN_LOGIN_MODAL__: (() => void) | null;
    }
}

interface AuthStore {
    // State
    user: User | null;
    accessToken: string | null;
    refreshToken: string | null;
    expiresAt: number | null;
    rememberMe: boolean;
    isLoginModalOpen: boolean;

    // Actions
    setAuth: (user: User | null, accessToken: string | null, refreshToken: string | null, rememberMe?: boolean) => void;
    clearAuth: () => void;
    openLoginModal: () => void;
    closeLoginModal: () => void;
}

export const useAuthStore = create<AuthStore>()(
    persist(
        (set) => ({
            // Initial state
            user: null,
            accessToken: null,
            refreshToken: null,
            expiresAt: null,
            rememberMe: false,
            isLoginModalOpen: false,

            // Set auth with automatic JWT decoding
            setAuth: (user, accessToken, refreshToken, rememberMe = false) =>
                set((state) => {
                    let finalUser = user;
                    let finalExpiresAt: number | null = null;

                    // Decode JWT if we have an access token
                    if (accessToken) {
                        const decoded = decodeJwt(accessToken);
                        if (decoded) {
                            if (!finalUser) finalUser = decoded.user;
                            finalExpiresAt = decoded.expiresAt;
                        }
                    }

                    return {
                        user: finalUser,
                        accessToken,
                        refreshToken,
                        rememberMe: rememberMe ?? state.rememberMe,
                        expiresAt: finalExpiresAt,
                    };
                }),

            // Clear all auth state
            clearAuth: () =>
                set({
                    user: null,
                    accessToken: null,
                    refreshToken: null,
                    expiresAt: null,
                    rememberMe: false,
                }),

            openLoginModal: () =>
                set((state) => (state.isLoginModalOpen ? state : { ...state, isLoginModalOpen: true })),

            closeLoginModal: () =>
                set((state) => (state.isLoginModalOpen ? { ...state, isLoginModalOpen: false } : state)),
        }),
        {
            name: 'auth-storage',
            // Custom storage: localStorage for "remember me", sessionStorage otherwise
            storage: {
                getItem: (name) => {
                    const str = localStorage.getItem(name) || sessionStorage.getItem(name);
                    return str ? JSON.parse(str) : null;
                },
                setItem: (name, value: any) => {
                    const str = JSON.stringify(value);
                    if (value.state.rememberMe) {
                        localStorage.setItem(name, str);
                        sessionStorage.removeItem(name);
                    } else {
                        sessionStorage.setItem(name, str);
                        localStorage.removeItem(name);
                    }
                },
                removeItem: (name) => {
                    localStorage.removeItem(name);
                    sessionStorage.removeItem(name);
                },
            },
            // Persist these fields across page refreshes
            partialize: (state) => ({
                user: state.user,
                accessToken: state.accessToken,
                refreshToken: state.refreshToken,
                rememberMe: state.rememberMe,
                expiresAt: state.expiresAt,
            }),
        }
    )
);
