export interface SubAgentProgressEvent {
  agentType: 'explorer' | 'sql_planner';
  phase: 'start' | 'progress' | 'complete' | 'error';
  message?: string;
  /** Tool usage stats (present on 'complete' phase). */
  toolCount?: number;
  toolCounts?: Record<string, number>;
  /** Identifies which parallel SubAgent task this event belongs to. */
  taskId?: string;
  /** Stable connection identity from backend lifecycle events. */
  connectionId?: number;
  /** Real task-scoped result summary from backend SUB_AGENT_COMPLETE. */
  summaryText?: string;
  /** Real task-scoped result payload from backend SUB_AGENT_COMPLETE. */
  resultJson?: string;
}

export interface ResolvedSubAgentResult {
  summaryText?: string;
  resultJson?: string;
}

function buildExplorerPayload(raw: Record<string, unknown>): string | undefined {
  const payload: Record<string, unknown> = {};
  if (typeof raw.status === 'string') {
    payload.status = raw.status;
  }
  if (Array.isArray(raw.objects)) {
    payload.objects = raw.objects;
  }
  if (typeof raw.summaryText === 'string') {
    payload.summaryText = raw.summaryText;
  }
  if (typeof raw.errorMessage === 'string') {
    payload.errorMessage = raw.errorMessage;
  }
  if (typeof raw.rawResponse === 'string') {
    payload.rawResponse = raw.rawResponse;
  }
  return Object.keys(payload).length > 0 ? JSON.stringify(payload) : undefined;
}

function buildPlannerPayload(raw: Record<string, unknown>): string | undefined {
  const payload: Record<string, unknown> = {};
  if (typeof raw.summaryText === 'string') {
    payload.summaryText = raw.summaryText;
  }
  if (Array.isArray(raw.planSteps)) {
    payload.planSteps = raw.planSteps;
  }
  if (Array.isArray(raw.sqlBlocks)) {
    payload.sqlBlocks = raw.sqlBlocks;
  }
  if (typeof raw.rawResponse === 'string') {
    payload.rawResponse = raw.rawResponse;
  }
  return Object.keys(payload).length > 0 ? JSON.stringify(payload) : undefined;
}

const VALID_AGENT_TYPES = new Set(['explorer', 'sql_planner']);
const VALID_PHASES = new Set(['start', 'progress', 'complete', 'error']);
const SUB_AGENT_TOOL_NAMES = new Set(['callingExplorerSubAgent', 'callingPlannerSubAgent']);

export function isCallingSubAgentTool(name: string): boolean {
  return SUB_AGENT_TOOL_NAMES.has(name);
}

/** Derive agentType from tool name (exploreSchema → explorer, generateSqlPlan → sql_planner). */
export function agentTypeFromToolName(toolName: string): 'explorer' | 'sql_planner' | null {
  if (toolName === 'callingExplorerSubAgent') return 'explorer';
  if (toolName === 'callingPlannerSubAgent') return 'sql_planner';
  return null;
}

/**
 * Try to parse a STATUS block's data as a SubAgent progress event.
 * Returns null if data is not a valid SubAgent progress JSON.
 */
export function tryParseSubAgentProgress(data: string | undefined): SubAgentProgressEvent | null {
  if (!data) return null;
  try {
    const parsed = JSON.parse(data) as Record<string, unknown>;
    if (
      typeof parsed.agentType === 'string' &&
      VALID_AGENT_TYPES.has(parsed.agentType) &&
      typeof parsed.phase === 'string' &&
      VALID_PHASES.has(parsed.phase)
    ) {
      return {
        agentType: parsed.agentType as SubAgentProgressEvent['agentType'],
        phase: parsed.phase as SubAgentProgressEvent['phase'],
        message: typeof parsed.message === 'string' ? parsed.message : undefined,
        toolCount: typeof parsed.toolCount === 'number' ? parsed.toolCount : undefined,
        toolCounts: parsed.toolCounts && typeof parsed.toolCounts === 'object'
          ? parsed.toolCounts as Record<string, number>
          : undefined,
        taskId: typeof parsed.taskId === 'string' ? parsed.taskId : undefined,
        connectionId: typeof parsed.connectionId === 'number' ? parsed.connectionId : undefined,
        summaryText: typeof parsed.summaryText === 'string' ? parsed.summaryText : undefined,
        resultJson: typeof parsed.resultJson === 'string' ? parsed.resultJson : undefined,
      };
    }
  } catch (error) {
    console.warn("[SubAgent] progress parse failed", { data, error });
  }
  return null;
}

export interface CallingSubAgentArgs {
  agentType: string;
  userQuestion?: string;
  connectionIds?: number[];
  taskCount?: number;
}

/**
 * Parse the arguments of callingExplorerSubAgent / callingPlannerSubAgent TOOL_CALL.
 * agentType is derived from toolName.
 *
 * Explorer new format: { tasksJson: "[{connectionId:1, instruction:...}, ...]", timeoutSeconds?: N }
 * Planner format: { instruction: "...", schemaSummaryJson: "..." }
 */
export function parseCallingSubAgentArgs(
  args: string,
  toolName?: string
): CallingSubAgentArgs | null {
  const derivedAgentType = toolName ? agentTypeFromToolName(toolName) : null;
  if (!args && !derivedAgentType) return null;

  try {
    let parsed: unknown = args && args.trim() ? JSON.parse(args) : {};
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return derivedAgentType ? { agentType: derivedAgentType } : null;
    }
    const obj = parsed as Record<string, unknown>;

    const agentType =
      typeof obj.agentType === 'string'
        ? obj.agentType
        : derivedAgentType ?? 'unknown';

    // Extract connectionIds from tasksJson (Explorer new format)
    let connectionIds: number[] | undefined;
    let taskCount: number | undefined;
    if (typeof obj.tasksJson === 'string') {
      try {
        const tasks = JSON.parse(obj.tasksJson) as unknown[];
        if (Array.isArray(tasks)) {
          taskCount = tasks.length;
          connectionIds = tasks
            .map((t) => (t && typeof t === 'object' && 'connectionId' in t) ? (t as Record<string, unknown>).connectionId : null)
            .filter((id): id is number => typeof id === 'number');
        }
      } catch { /* tasksJson not yet complete */ }
    }

    // Fallback: legacy connectionIds field
    if (!connectionIds && Array.isArray(obj.connectionIds)) {
      connectionIds = (obj.connectionIds as unknown[]).filter((r): r is number => typeof r === 'number');
    }

    return {
      agentType,
      userQuestion: typeof obj.instruction === 'string' ? obj.instruction : undefined,
      connectionIds: connectionIds?.length ? connectionIds : undefined,
      taskCount,
    };
  } catch {
    // Streaming: args arrive as partial JSON — silently fall back
    if (derivedAgentType) return { agentType: derivedAgentType };
  }
  return null;
}

/**
 * Extract the real summaryText from the SubAgent TOOL_RESULT when present.
 */
export function getSubAgentResultSummary(agentType: string, responseData: string): string {
  return resolveSubAgentResult(agentType, responseData).summaryText ?? '';
}

export function resolveSubAgentResult(
  agentType: string,
  responseData: string,
  taskId?: string
): ResolvedSubAgentResult {
  if (!responseData) return {};
  try {
    let parsed: unknown = JSON.parse(responseData);
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {};
    const obj = parsed as Record<string, unknown>;

    // Unwrap { result: "...", _trace: ... } wrapper
    let inner = obj;
    if (typeof obj.result === 'string') {
      try {
        const innerParsed = JSON.parse(obj.result) as unknown;
        if (innerParsed && typeof innerParsed === 'object' && !Array.isArray(innerParsed)) {
          inner = innerParsed as Record<string, unknown>;
        }
      } catch { /* use obj directly */ }
    }

    const normalized = agentType.toUpperCase().trim();
    if (normalized === 'EXPLORER' || agentType === 'explorer') {
      if (Array.isArray(inner.taskResults)) {
        const taskResults = inner.taskResults.filter((item): item is Record<string, unknown> => !!item && typeof item === 'object');
        const matchedTask = taskId
          ? taskResults.find((item) => item.taskId === taskId)
          : taskResults.length === 1 ? taskResults[0] : undefined;
        if (matchedTask) {
          return {
            summaryText: typeof matchedTask.summaryText === 'string' ? matchedTask.summaryText : undefined,
            resultJson: buildExplorerPayload(matchedTask),
          };
        }
        if (!taskId && taskResults.length > 0) {
          return {
            summaryText: `Completed ${taskResults.length} exploration task${taskResults.length === 1 ? '' : 's'}.`,
            resultJson: responseData,
          };
        }
        return {};
      }

      if (Array.isArray(inner.objects) || typeof inner.rawResponse === 'string') {
        return {
          summaryText: typeof inner.summaryText === 'string' ? inner.summaryText : undefined,
          resultJson: buildExplorerPayload(inner),
        };
      }

      const tables = Array.isArray(inner.tables) ? inner.tables : [];
      const colCount = tables.reduce((sum: number, t: unknown) => {
        if (t && typeof t === 'object' && 'columns' in t && Array.isArray((t as Record<string, unknown>).columns)) {
          return sum + ((t as Record<string, unknown>).columns as unknown[]).length;
        }
        return sum;
      }, 0);
      if (tables.length === 0 && colCount === 0) return {};
      return {
        summaryText: `Found ${tables.length} table${tables.length !== 1 ? 's' : ''}, ${colCount} column${colCount !== 1 ? 's' : ''}`,
        resultJson: JSON.stringify(inner),
      };
    }

    if (normalized === 'SQL_PLANNER' || agentType === 'sql_planner') {
      if (
        typeof inner.summaryText === 'string'
        || Array.isArray(inner.planSteps)
        || Array.isArray(inner.sqlBlocks)
        || typeof inner.rawResponse === 'string'
      ) {
        return {
          summaryText: typeof inner.summaryText === 'string' ? inner.summaryText : undefined,
          resultJson: buildPlannerPayload(inner),
        };
      }
    }
  } catch {
    // Parse failed
  }
  return {};
}
