import { useMemo, useState } from 'react';
import { CheckCircle2, ChevronDown, ChevronRight, Loader2, XCircle } from 'lucide-react';
import { cn } from '../../../lib/utils';
import type { Segment } from '../messageListLib/types';
import { SegmentKind, ToolExecutionState } from '../messageListLib/types';
import { getToolDisplayName } from './sqlDiscoveryToolUtils';
import { formatParameters } from './formatParameters';
import { ToolRunDetail } from './ToolRunDetail';

export interface BackgroundToolGroupBlockProps {
  nestedToolRuns: Segment[];
  pending?: boolean;
  startedAt?: number;
  finishedAt?: number;
}

type ToolRunSegment = Extract<Segment, { kind: SegmentKind.TOOL_RUN }>;

const MAX_DESCRIPTION_LENGTH = 80;

function toolRunsOnly(segments: Segment[]): ToolRunSegment[] {
  return segments.filter((segment): segment is ToolRunSegment => segment.kind === SegmentKind.TOOL_RUN);
}

export function sanitizeToolDescription(description: string | undefined, fallback: string): string {
  const raw = (description ?? fallback).replace(/\s+/g, ' ').trim();
  if (raw.length <= MAX_DESCRIPTION_LENGTH) return raw;
  return `${raw.slice(0, MAX_DESCRIPTION_LENGTH - 3)}...`;
}

function isRunning(segment: ToolRunSegment): boolean {
  return segment.pending === true
    || segment.executionState === ToolExecutionState.EXECUTING
    || segment.executionState === ToolExecutionState.STREAMING_ARGUMENTS;
}

function formatDuration(startedAt?: number, finishedAt?: number): string | undefined {
  if (typeof startedAt !== 'number' || typeof finishedAt !== 'number') {
    return undefined;
  }
  const durationMs = Math.max(0, finishedAt - startedAt);
  if (!Number.isFinite(durationMs)) {
    return undefined;
  }
  return `${(durationMs / 1000).toFixed(1)}s`;
}

interface BackgroundToolStepProps {
  segment: ToolRunSegment;
  index: number;
}

function BackgroundToolStep({ segment, index }: BackgroundToolStepProps) {
  const [collapsed, setCollapsed] = useState(true);
  const stepRunning = isRunning(segment);
  const stepLabel = sanitizeToolDescription(segment.description, getToolDisplayName(segment.toolName));
  const keyId = segment.toolCallId ?? `${segment.toolName}-${index}`;

  return (
    <div className="overflow-hidden rounded-md border theme-border bg-black/[0.02] dark:bg-white/[0.02]">
      <button
        type="button"
        onClick={() => setCollapsed((current) => !current)}
        className="flex w-full items-start gap-2 px-2 py-1.5 text-left text-[12px] theme-text-primary transition-colors hover:bg-black/5 dark:hover:bg-white/[0.04]"
        aria-expanded={!collapsed}
        aria-controls={`background-tool-step-${keyId}`}
      >
        {segment.responseError ? (
          <XCircle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-red-500" aria-label="Failed" />
        ) : stepRunning ? (
          <Loader2 className="mt-0.5 h-3.5 w-3.5 shrink-0 animate-spin text-blue-500" aria-hidden />
        ) : (
          <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0 text-green-500" aria-hidden />
        )}
        <span className="min-w-0 flex-1 break-words leading-5">{stepLabel}</span>
        <span className={cn(
          'shrink-0 rounded-full border theme-border px-1.5 py-0.5 text-[10px] theme-text-secondary',
          segment.responseError && 'text-red-500'
        )}>
          {segment.responseError ? 'Error' : stepRunning ? 'Running' : 'Done'}
        </span>
        <span className="mt-0.5 shrink-0 theme-text-secondary">
          {collapsed ? <ChevronRight className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div
          id={`background-tool-step-${keyId}`}
          className="border-t theme-border px-2 py-2"
        >
          <ToolRunDetail
            formattedParameters={formatParameters(segment.parametersData)}
            responseData={segment.responseData}
          />
        </div>
      )}
    </div>
  );
}

export function BackgroundToolGroupBlock({
  nestedToolRuns,
  pending = false,
  startedAt,
  finishedAt,
}: BackgroundToolGroupBlockProps) {
  const [collapsed, setCollapsed] = useState(true);
  const toolRuns = useMemo(() => toolRunsOnly(nestedToolRuns), [nestedToolRuns]);
  const completedCount = toolRuns.filter((segment) => !isRunning(segment)).length;
  const failedCount = toolRuns.filter((segment) => segment.responseError).length;
  const running = pending || toolRuns.some(isRunning);
  const duration = formatDuration(startedAt, finishedAt);
  const statusLabel = failedCount > 0 ? 'Failed' : running ? 'Running' : 'Done';
  const summaryText = running
    ? `Completed ${completedCount}/${toolRuns.length} steps${duration ? ` in ${duration}` : ''}`
    : duration
      ? `Completed ${completedCount} steps in ${duration}`
      : `Completed ${completedCount} steps`;

  return (
    <div className="mb-2 overflow-hidden rounded-lg border theme-border bg-[color:var(--bg-panel)]">
      <button
        type="button"
        onClick={() => setCollapsed((current) => !current)}
        className="flex w-full items-center gap-2.5 px-3 py-2 text-left transition-colors hover:bg-black/5 dark:hover:bg-white/[0.04]"
      >
        {failedCount > 0 ? (
          <XCircle className="h-3.5 w-3.5 shrink-0 text-red-500" aria-label="Failed" />
        ) : running ? (
          <Loader2 className="h-3.5 w-3.5 shrink-0 animate-spin text-blue-500" aria-hidden />
        ) : (
          <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-green-500" aria-hidden />
        )}
        <span className="min-w-0 flex-1 truncate text-[12px] font-medium theme-text-primary">
          {summaryText}
        </span>
        <span className="shrink-0 rounded-full border theme-border px-2 py-0.5 text-[10px] font-medium theme-text-secondary">
          {statusLabel}
        </span>
        <span className="shrink-0 theme-text-secondary">
          {collapsed ? <ChevronRight className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div className="border-t theme-border px-3 py-2">
          <div className="space-y-1.5">
            {toolRuns.map((segment, index) => (
              <BackgroundToolStep
                key={segment.toolCallId ?? `${segment.toolName}-${index}`}
                segment={segment}
                index={index}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
