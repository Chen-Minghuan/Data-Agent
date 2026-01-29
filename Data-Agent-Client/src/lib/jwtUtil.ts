import { User } from '../types/auth';

/**
 * JWT payload structure from backend
 */
interface JwtPayload {
    loginId: number;
    username: string;
    email: string;
    eff: number; // Expiration time in milliseconds
    loginType: string;
    deviceType: string;
    rnStr: string;
}

/**
 * Decoded JWT result
 */
export interface DecodedJwt {
    user: User;
    expiresAt: number;
}

/**
 * Decode JWT token and extract user info and expiration time.
 * @param token - JWT access token string
 * @returns Decoded user and expiration, or null if decode fails
 */
export const decodeJwt = (token: string): DecodedJwt | null => {
    try {
        // Extract payload (second part of JWT)
        const base64Url = token.split('.')[1];
        if (!base64Url) {
            throw new Error('Invalid JWT format');
        }

        // Convert base64url to base64
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

        // Decode base64 to JSON string
        const jsonPayload = decodeURIComponent(
            window.atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );

        const payload: JwtPayload = JSON.parse(jsonPayload);

        // Map JWT payload to User object
        const user: User = {
            id: payload.loginId,
            username: payload.username,
            email: payload.email,
        };

        return {
            user,
            expiresAt: payload.eff,
        };
    } catch (e) {
        console.error('Failed to decode JWT:', e);
        return null;
    }
};

/**
 * Check if JWT token is already expired.
 * @param expiresAt - Expiration timestamp in milliseconds
 * @returns True if token is expired
 */
export const isTokenExpired = (expiresAt: number): boolean => {
    return Date.now() >= expiresAt;
};
