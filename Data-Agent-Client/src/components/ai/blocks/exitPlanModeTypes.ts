/** Mirrors backend ExitPlanResult */
export interface ExitPlanPayload {
  title: string;
  steps: PlanStep[];
  risks: string[];
}

export interface PlanStep {
  order: number;
  description: string;
  sql: string;
  objectName: string;
}

export const EXIT_PLAN_MODE_TOOL_NAME = 'exitPlanMode';

export function isExitPlanModeTool(toolName: string): boolean {
  return toolName === EXIT_PLAN_MODE_TOOL_NAME;
}

export function parseExitPlanPayload(
  responseData: string | null | undefined
): ExitPlanPayload | null {
  if (!responseData?.trim()) return null;
  try {
    const parsed = JSON.parse(responseData.trim()) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    if (typeof obj.title !== 'string') return null;
    return {
      title: obj.title,
      steps: Array.isArray(obj.steps) ? obj.steps : [],
      risks: Array.isArray(obj.risks) ? obj.risks : [],
    };
  } catch {
    return null;
  }
}

