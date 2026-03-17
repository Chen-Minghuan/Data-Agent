import { useEffect, useRef } from 'react';
import { useWorkspaceStore } from '../../../store/workspaceStore';
import type { SubAgentConsoleTabMetadata, SubAgentInvocation } from '../../../types/tab';
import { subAgentConsoleTabId } from './subAgentDataHelpers';

export interface UseSubAgentConsoleTabOptions {
  enabled?: boolean;
  toolCallId?: string;
  taskKey?: string;
  conversationId: number | null;
  agentType: 'explorer' | 'sql_planner';
  taskLabel: string;
  status: 'running' | 'complete' | 'error';
  startedAt: number;
  completedAt?: number;
  params?: SubAgentConsoleTabMetadata['params'];
  summary?: string;
  resultJson?: string;
  invocations: SubAgentInvocation[];
}

export function useSubAgentConsoleTab(options: UseSubAgentConsoleTabOptions) {
  const {
    enabled = true,
    toolCallId,
    taskKey,
    conversationId,
    agentType,
    taskLabel,
    status,
    startedAt,
    completedAt,
    params,
    summary,
    resultJson,
    invocations,
  } = options;

  const lastMetadataKeyRef = useRef('');
  const fallbackToolCallIdRef = useRef(`subagent-${Date.now()}`);
  const stableToolCallId = toolCallId ?? fallbackToolCallIdRef.current;
  const tabId = subAgentConsoleTabId(stableToolCallId, taskKey);
  const metadataRef = useRef<SubAgentConsoleTabMetadata | null>(null);
  const autoOpenedRef = useRef(false);

  useEffect(() => {
    if (!enabled) return;
    const ws = useWorkspaceStore.getState();

    const metadata: SubAgentConsoleTabMetadata = {
      conversationId,
      agentType,
      status,
      startedAt,
      completedAt,
      params,
      summary,
      resultJson,
      invocations,
    };
    metadataRef.current = metadata;

    const metadataKey = [
      status,
      completedAt ?? '',
      summary ?? '',
      resultJson ?? '',
      ...invocations.map((invocation) => [
        invocation.id,
        invocation.status,
        invocation.resultSummary ?? '',
        invocation.resultJson ?? '',
        invocation.nestedToolRuns?.map((segment) => {
          if (segment.kind !== 'TOOL_RUN') return segment.kind;
          return [
            segment.toolName,
            segment.parametersData,
            segment.responseData,
            segment.responseError ? 'error' : '',
            segment.executionState ?? '',
          ].join('|');
        }).join('~') ?? '',
        invocation.nestedToolCalls?.map((tool) => `${tool.toolName}:${tool.status}`).join('|') ?? '',
      ].join(':')),
    ].join('||');

    if (metadataKey === lastMetadataKeyRef.current) return;
    lastMetadataKeyRef.current = metadataKey;

    const existingTab = ws.tabs.find((tab) => tab.id === tabId);
    if (existingTab) {
      ws.updateSubAgentConsole(tabId, metadata);
      return;
    }

    if (status === 'running' && !autoOpenedRef.current) {
      autoOpenedRef.current = true;
      ws.openTab({
        id: tabId,
        name: `${taskLabel} Console`,
        type: 'subagent-console',
        metadata,
      });
    }
  }, [agentType, completedAt, conversationId, enabled, invocations, params, resultJson, stableToolCallId, startedAt, status, summary, tabId, taskKey, taskLabel]);

  const handleOpenConsole = () => {
    if (!enabled || !metadataRef.current) return;
    const ws = useWorkspaceStore.getState();
    const existingTab = ws.tabs.find((tab) => tab.id === tabId);
    if (existingTab) {
      ws.updateSubAgentConsole(tabId, metadataRef.current);
      ws.switchTab(tabId);
      return;
    }
    ws.openTab({
      id: tabId,
      name: `${taskLabel} Console`,
      type: 'subagent-console',
      metadata: metadataRef.current,
    });
  };

  return { tabId, handleOpenConsole };
}
