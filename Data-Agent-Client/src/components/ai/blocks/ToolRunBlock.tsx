import { TodoListBlock } from './TodoListBlock';
import { ToolRunStreaming } from './ToolRunStreaming';
import { ToolRunExecuting } from './ToolRunExecuting';
import { GenericToolRun } from './GenericToolRun';
import { ChartToolBlock } from './ChartToolBlock';
import { parseTodoListResponse } from './todoTypes';
import {
  parseAskUserQuestionParameters,
  parseAskUserQuestionResponse,
} from './askUserQuestionTypes';
import { parseWriteConfirmPayload, parseWriteConfirmToken } from './writeConfirmTypes';
import { parseExitPlanPayload } from './exitPlanModeTypes';
import { getToolType, ToolType } from './toolTypes';
import { formatParameters } from './formatParameters';
import { ToolExecutionState } from '../messageListLib/types';
import { AskUserQuestionCard } from './AskUserQuestionCard';
import { WriteConfirmCard } from './WriteConfirmCard';
import { ExitPlanModeCard } from './ExitPlanModeCard';
import { ThoughtBlock } from './ThoughtBlock';

export interface ToolRunBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  /** True when tool execution failed (backend ToolExecution.hasFailed()). */
  responseError?: boolean;
  /** True while waiting for TOOL_RESULT (no icon, tool name blinks). */
  pending?: boolean;
  /** Execution state: streaming arguments, executing, or complete. */
  executionState?: ToolExecutionState;
  /** Tool call id from TOOL_CALL block; used for pairing and retry dedupe. */
  toolCallId?: string;
  /** Whether this tool run belongs to current streaming turn and can auto retry. */
  allowAutoRetry?: boolean;
}

/**
 * Renders a single tool execution result.
 *
 * Tool types:
 * - TODO: TodoWrite → TodoListBlock
 * - ASK_USER: AskUserQuestion → AskUserQuestionCard (Inline)
 * - WRITE_CONFIRM: AskUserConfirm → WriteConfirmCard (Inline)
 * - GENERIC: All other tools (database, etc.) → ToolRunDetail
 */
export function ToolRunBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
  pending = false,
  executionState,
  toolCallId,
  allowAutoRetry = false,
}: ToolRunBlockProps) {
  const toolType = getToolType(toolName);
  const formattedParameters = formatParameters(parametersData);
  const isInteractive = toolType === ToolType.ASK_USER || toolType === ToolType.WRITE_CONFIRM;

  // 0a. Thinking tool — render as thought block at every lifecycle stage
  if (toolType === ToolType.THINKING) {
    const isStreaming = executionState === ToolExecutionState.STREAMING_ARGUMENTS
      || executionState === ToolExecutionState.EXECUTING
      || (pending && !executionState);
    const analysis = extractThinkingAnalysis(parametersData);
    if (!analysis) return null;
    return <ThoughtBlock data={analysis} defaultExpanded={isStreaming} />;
  }

  // 0b. ExitPlanMode — stream as thought block, render card when complete
  if (toolType === ToolType.EXIT_PLAN) {
    const isStreaming = executionState === ToolExecutionState.STREAMING_ARGUMENTS
      || executionState === ToolExecutionState.EXECUTING
      || (pending && !executionState);
    if (isStreaming) {
      // During streaming, show raw arguments as a thought block (like thinking tool)
      const analysis = extractThinkingAnalysis(parametersData);
      if (!analysis) return null;
      return <ThoughtBlock data={analysis} defaultExpanded={true} />;
    }
    // Complete — render the full plan card
    const payload = parseExitPlanPayload(parametersData);
    if (!payload) return null;
    return <ExitPlanModeCard payload={payload} />;
  }

  // 1. Handle Execution Lifecycle States
  if (executionState === ToolExecutionState.STREAMING_ARGUMENTS) {
    if (isInteractive) return <ToolRunExecuting toolName={toolName} parametersData={parametersData} />;
    return <ToolRunStreaming toolName={toolName} partialArguments={parametersData} />;
  }

  if (executionState === ToolExecutionState.EXECUTING || (pending && !executionState)) {
    // Both interactive and non-interactive tools should just pulse while executing/pending
    return <ToolRunExecuting toolName={toolName} parametersData={parametersData} />;
  }

  // 2. Error fallback for non-chart tools.
  // Chart errors should still go through ChartToolBlock to trigger auto-feedback.
  if (responseError && toolType !== ToolType.CHART) {
    return (
      <GenericToolRun
        toolName={toolName}
        formattedParameters={formattedParameters}
        responseData={responseData}
        responseError={responseError}
      />
    );
  }

  // 3. Dispatch Completed Tool Rendering by Category
  switch (toolType) {
    // CATEGORY A: Interactive Tools (Inline Cards)
    case ToolType.ASK_USER: {
      const askUserPayloadFromResponse = parseAskUserQuestionResponse(responseData);
      const askUserPayloadFromParams = parseAskUserQuestionParameters(parametersData);
      const askUserPayload = askUserPayloadFromResponse ?? askUserPayloadFromParams ?? null;

      // Detect if responseData is a user's submitted answer (not a minimal tool summary).
      // Minimal summaries like "2 question(s) presented to user." are NOT user answers.
      const responseText = (responseData ?? '').trim();
      const isMinimalToolSummary = /^\d+ question\(s\) presented to user\.$/.test(responseText);
      const askUserSubmittedAnswer =
        askUserPayloadFromResponse == null && askUserPayloadFromParams != null && responseText !== '' && !isMinimalToolSummary
          ? responseText
          : undefined;

      if (!askUserPayload) return null;

      // If already answered but not loading, or if we need to show the form
      // We always render the card (interactive if questions to answer, static if answered)
      return (
        <AskUserQuestionCard
          askUserPayload={askUserPayload}
          submittedAnswer={askUserSubmittedAnswer}
        />
      );
    }

    case ToolType.WRITE_CONFIRM: {
      // Display data (sql, explanation) is in tool call arguments;
      // confirmationToken is in tool result (generated server-side).
      const fromParams = parseWriteConfirmPayload(parametersData);
      const fromResponse = parseWriteConfirmPayload(responseData);
      const tokenOnly = parseWriteConfirmToken(responseData);

      // Merge: prefer params for display data, fill in token from response
      let writeConfirmPayload = fromResponse ?? fromParams ?? null;
      if (fromParams && tokenOnly) {
        writeConfirmPayload = { ...fromParams, confirmationToken: tokenOnly.confirmationToken, expiresInSeconds: tokenOnly.expiresInSeconds };
      }
      if (!writeConfirmPayload) return null;

      // Determine if it was already answered checking responseData text if parameters held the token
      let submittedAnswer: string | undefined = undefined;
      if (responseData && (responseData.includes('confirmed') || responseData.includes('cancelled'))) {
        submittedAnswer = responseData;
      }

      return (
        <WriteConfirmCard
          payload={writeConfirmPayload}
          submittedAnswer={submittedAnswer}
        />
      );
    }

    // CATEGORY B: Specialized Content Presentation
    case ToolType.TODO: {
      const todoItems = parseTodoListResponse(responseData)?.items ?? null;
      if (!todoItems) return null;
      return (
        <div className="mb-2">
          <TodoListBlock items={todoItems} />
        </div>
      );
    }

    case ToolType.CHART:
      return (
        <ChartToolBlock
          toolName={toolName}
          parametersData={parametersData}
          responseData={responseData}
          responseError={responseError}
          toolCallId={toolCallId}
          allowAutoRetry={allowAutoRetry}
        />
      );

    // CATEGORY C: Generic Data Fetching / DB Tools
    case ToolType.GENERIC:
    default:
      return (
        <GenericToolRun
          toolName={toolName}
          formattedParameters={formattedParameters}
          responseData={responseData}
          responseError={responseError}
        />
      );
  }
}

/** Extract the "analysis" field from sequentialThinking tool call arguments. */
function extractThinkingAnalysis(parametersData: string): string | null {
  if (!parametersData) return null;
  try {
    let parsed: unknown = JSON.parse(parametersData);
    // Handle double-encoded JSON string
    if (typeof parsed === 'string') parsed = JSON.parse(parsed) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const obj = parsed as Record<string, unknown>;
    // Try nested request.analysis first, then top-level analysis
    if (obj.request && typeof obj.request === 'object') {
      const req = obj.request as Record<string, unknown>;
      if (typeof req.analysis === 'string' && req.analysis) return req.analysis;
    }
    if (typeof obj.analysis === 'string' && obj.analysis) return obj.analysis;
    // Fallback: if only goal is available during streaming, show that
    if (typeof obj.goal === 'string' && obj.goal) return obj.goal;
    if (obj.request && typeof obj.request === 'object') {
      const req = obj.request as Record<string, unknown>;
      if (typeof req.goal === 'string' && req.goal) return req.goal;
    }
    return null;
  } catch {
    // During streaming, parametersData may be partial/incomplete JSON.
    // Try to extract analysis text with regex as fallback.
    const match = parametersData.match(/"analysis"\s*:\s*"((?:[^"\\]|\\.)*)"/);
    if (match?.[1]) return match[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
    const goalMatch = parametersData.match(/"goal"\s*:\s*"((?:[^"\\]|\\.)*)"/);
    if (goalMatch?.[1]) return goalMatch[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
    return null;
  }
}
