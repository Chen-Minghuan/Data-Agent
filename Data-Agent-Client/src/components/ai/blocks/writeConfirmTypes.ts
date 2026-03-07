/** Mirrors backend AskUserConfirmTool.WriteConfirmationResult */
export interface WriteConfirmPayload {
  confirmationToken: string;
  sqlPreview: string;
  explanation: string;
  connectionId: number;
  databaseName?: string | null;
  schemaName?: string | null;
  expiresInSeconds: number;
  error?: boolean;
  errorMessage?: string;
}

export const WRITE_CONFIRM_TOOL_NAME = 'askUserConfirm';

export function isWriteConfirmTool(toolName: string): boolean {
  return toolName === WRITE_CONFIRM_TOOL_NAME;
}

/**
 * Parse a minimal token-only result (from optimized backend that no longer echoes display data).
 * Returns { confirmationToken, expiresInSeconds } or null.
 */
export function parseWriteConfirmToken(
  responseData: string | null | undefined
): Pick<WriteConfirmPayload, 'confirmationToken' | 'expiresInSeconds'> | null {
  if (!responseData?.trim()) return null;
  try {
    const parsed = JSON.parse(responseData.trim()) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    if (typeof obj.confirmationToken !== 'string' || !obj.confirmationToken) return null;
    return {
      confirmationToken: obj.confirmationToken,
      expiresInSeconds: typeof obj.expiresInSeconds === 'number' ? obj.expiresInSeconds : 300,
    };
  } catch {
    return null;
  }
}

/**
 * Parse WriteConfirmPayload from JSON data.
 * Accepts both tool result format (sqlPreview, confirmationToken) and
 * tool call parameter format (sql instead of sqlPreview, no token).
 */
export function parseWriteConfirmPayload(
  responseData: string | null | undefined
): WriteConfirmPayload | null {
  if (!responseData?.trim()) return null;
  try {
    const parsed = JSON.parse(responseData.trim()) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;

    // Must have either confirmationToken (result) or sql/explanation (parameters)
    const hasToken = typeof obj.confirmationToken === 'string' && !!obj.confirmationToken;
    const hasSql = typeof obj.sql === 'string' || typeof obj.sqlPreview === 'string';
    if (!hasToken && !hasSql) return null;

    const payload: WriteConfirmPayload = {
      confirmationToken: typeof obj.confirmationToken === 'string' ? obj.confirmationToken : '',
      // Accept both "sqlPreview" (legacy result) and "sql" (tool call parameter name)
      sqlPreview: typeof obj.sqlPreview === 'string' ? obj.sqlPreview : (typeof obj.sql === 'string' ? obj.sql : ''),
      explanation: typeof obj.explanation === 'string' ? obj.explanation : '',
      connectionId: typeof obj.connectionId === 'number' ? obj.connectionId : 0,
      expiresInSeconds: typeof obj.expiresInSeconds === 'number' ? obj.expiresInSeconds : 300,
      error: typeof obj.error === 'boolean' ? obj.error : undefined,
      errorMessage: typeof obj.errorMessage === 'string' ? obj.errorMessage : undefined,
    };
    if (typeof obj.databaseName === 'string') {
      payload.databaseName = obj.databaseName;
    } else if (obj.databaseName === null) {
      payload.databaseName = null;
    }
    if (typeof obj.schemaName === 'string') {
      payload.schemaName = obj.schemaName;
    } else if (obj.schemaName === null) {
      payload.schemaName = null;
    }
    return payload;
  } catch {
    return null;
  }
}
