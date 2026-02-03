export interface DbConnection {
    id: number;
    name: string;
    dbType: string;
    host: string;
    port: number;
    database?: string;
    username?: string;
    password?: string;
    driverJarPath: string;
    timeout?: number;
    properties?: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface CreateConnectionRequest {
    name: string;
    dbType: string;
    host: string;
    port: number;
    database?: string;
    username?: string;
    password?: string;
    driverJarPath: string;
    timeout?: number;
    properties?: string;
}

export interface TestConnectionRequest extends CreateConnectionRequest {}
