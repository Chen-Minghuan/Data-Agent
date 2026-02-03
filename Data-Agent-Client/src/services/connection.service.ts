import http from '../lib/http';
import { DbConnection, CreateConnectionRequest, TestConnectionRequest } from '../types/connection';

export const connectionService = {
    /**
     * List all connections
     */
    getConnections: async (): Promise<DbConnection[]> => {
        const response = await http.get<DbConnection[]>('/connections');
        return response.data;
    },

    /**
     * Get connection by ID
     */
    getConnectionById: async (id: number): Promise<DbConnection> => {
        const response = await http.get<DbConnection>(`/connections/${id}`);
        return response.data;
    },

    /**
     * Create a new connection
     */
    createConnection: async (data: CreateConnectionRequest): Promise<DbConnection> => {
        const response = await http.post<DbConnection>('/connections/create', data);
        return response.data;
    },

    /**
     * Update connection
     */
    updateConnection: async (id: number, data: CreateConnectionRequest): Promise<DbConnection> => {
        const response = await http.put<DbConnection>(`/connections/${id}`, data);
        return response.data;
    },

    /**
     * Delete connection
     */
    deleteConnection: async (id: number): Promise<void> => {
        await http.delete(`/connections/${id}`);
    },

    /**
     * Test connection
     */
    testConnection: async (data: TestConnectionRequest): Promise<boolean> => {
        const response = await http.post<boolean>('/connections/test', data);
        return response.data;
    },

    /**
     * Open a persistent connection
     */
    openConnection: async (id: number): Promise<string> => {
        const response = await http.post<{ connectionId: string }>('/connections/open', { id });
        return response.data.connectionId;
    }
};
