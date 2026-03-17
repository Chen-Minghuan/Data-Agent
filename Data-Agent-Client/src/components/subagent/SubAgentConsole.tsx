import type { ReactNode } from 'react';
import { Braces, CheckCircle, ChevronRight, Database, Loader2, XCircle } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { cn } from '../../lib/utils';
import { SUB_AGENT_LABELS } from '../../constants/chat';
import type { SubAgentConsoleTabMetadata, SubAgentInvocation } from '../../types/tab';
import { ToolRunBlock, markdownRemarkPlugins, useMarkdownComponents } from '../ai/blocks';
import { SegmentKind, ToolExecutionState } from '../ai/messageListLib/types';
import { SqlCodeBlock } from '../common/SqlCodeBlock';

export interface SubAgentConsoleProps {
  tabId: string;
  metadata: SubAgentConsoleTabMetadata;
}

function getStatusIcon(status: 'pending' | 'running' | 'complete' | 'error', className?: string) {
  if (status === 'complete') return <CheckCircle className={cn('w-4 h-4 text-green-500', className)} />;
  if (status === 'error') return <XCircle className={cn('w-4 h-4 text-red-500', className)} />;
  return <Loader2 className={cn('w-4 h-4 animate-spin theme-text-secondary', className)} />;
}

function getInvocationStatusText(invocation: SubAgentInvocation): string {
  const tools = invocation.nestedToolCalls ?? [];
  const completed = tools.filter((tool) => tool.status === 'complete').length;
  const runningTool = [...tools].reverse().find((tool) => tool.status === 'running');
  const lastCompletedTool = [...tools].reverse().find((tool) => tool.status === 'complete');
  const failedTool = [...tools].reverse().find((tool) => tool.responseError);

  if (invocation.status === 'error') {
    return failedTool ? `Failed at ${failedTool.toolName}` : 'Agent failed';
  }
  if (invocation.status === 'complete') {
    return 'Complete';
  }
  if (runningTool) {
    return `Calling ${runningTool.toolName}... (${completed}/${tools.length})`;
  }
  if (tools.length > 0 && completed === tools.length) {
    return 'Starting summary...';
  }
  if (lastCompletedTool) {
    return `Called ${lastCompletedTool.toolName}... (${completed}/${tools.length})`;
  }
  return 'Starting Agent...';
}

function ToolCalls({ invocation }: { invocation?: SubAgentInvocation }) {
  const toolRuns = invocation?.nestedToolRuns?.filter((segment) => segment.kind === SegmentKind.TOOL_RUN) ?? [];
  if (toolRuns.length === 0) {
    return <p className="text-[11px] theme-text-secondary">No tool calls yet.</p>;
  }

  return (
    <div className="space-y-2">
      {toolRuns.map((toolRun, index) => (
        <ToolRunBlock
          key={`${toolRun.toolCallId ?? toolRun.toolName}-${index}`}
          toolName={toolRun.toolName}
          parametersData={toolRun.parametersData}
          responseData={toolRun.responseData}
          responseError={toolRun.responseError}
          pending={toolRun.pending}
          executionState={
            toolRun.executionState === ToolExecutionState.STREAMING_ARGUMENTS
              ? ToolExecutionState.EXECUTING
              : toolRun.executionState
          }
          toolCallId={toolRun.toolCallId}
        />
      ))}
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border theme-border theme-bg-panel px-4 py-3">
      <h3 className="text-[11px] font-semibold uppercase tracking-[0.08em] theme-text-secondary mb-3">
        {title}
      </h3>
      {children}
    </section>
  );
}

function formatJson(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

interface ExplorerObjectPreview {
  catalog?: string;
  schema?: string;
  objectName: string;
  objectType?: string;
  objectDdl?: string;
  relevance?: string;
}

interface ExplorerResultPayload {
  objects: ExplorerObjectPreview[];
  rawResponse?: string;
}

function relevanceClass(relevance?: string): string {
  if (relevance === 'HIGH') return 'text-emerald-600 dark:text-emerald-400';
  if (relevance === 'MEDIUM') return 'text-amber-600 dark:text-amber-400';
  if (relevance === 'LOW') return 'text-rose-600 dark:text-rose-400';
  return 'theme-text-primary';
}

function relevanceBlockClass(relevance?: string): string {
  if (relevance === 'HIGH') return 'border-emerald-500/40 bg-emerald-500/12 hover:bg-emerald-500/18';
  if (relevance === 'MEDIUM') return 'border-amber-500/40 bg-amber-500/12 hover:bg-amber-500/18';
  if (relevance === 'LOW') return 'border-rose-500/40 bg-rose-500/12 hover:bg-rose-500/18';
  return 'theme-border theme-bg-tertiary';
}

function shouldExpandObjectDdl(relevance?: string): boolean {
  return relevance === 'HIGH';
}

function parseExplorerResult(resultJson?: string): ExplorerResultPayload {
  if (!resultJson) return { objects: [] };
  try {
    const parsed = JSON.parse(resultJson) as Record<string, unknown>;
    const objects = Array.isArray(parsed.objects) ? parsed.objects : [];
    return {
      objects: objects
        .filter((object): object is Record<string, unknown> => !!object && typeof object === 'object')
        .map((object) => ({
          catalog: typeof object.catalog === 'string' ? object.catalog : undefined,
          schema: typeof object.schema === 'string' ? object.schema : undefined,
          objectName: typeof object.objectName === 'string' ? object.objectName : 'unknown_object',
          objectType: typeof object.objectType === 'string' ? object.objectType : undefined,
          objectDdl: typeof object.objectDdl === 'string' ? object.objectDdl : undefined,
          relevance: typeof object.relevance === 'string' ? object.relevance : undefined,
        })),
      rawResponse: typeof parsed.rawResponse === 'string' ? parsed.rawResponse : undefined,
    };
  } catch {
    return { objects: [] };
  }
}

function ExplorerResultPreview({
  resultJson,
  rawResponseNode,
}: {
  resultJson?: string;
  rawResponseNode?: ReactNode;
}) {
  const result = parseExplorerResult(resultJson);
  if (result.objects.length === 0 && !result.rawResponse) return null;

  return (
    <div className="mt-4 space-y-5">
      {result.objects.map((object, index) => (
        <div key={`${object.catalog ?? ''}-${object.schema ?? ''}-${object.objectName}-${index}`}>
          {object.objectDdl ? (
            <details
              className="group"
              open={shouldExpandObjectDdl(object.relevance)}
            >
              <summary
                className={cn(
                  'list-none cursor-pointer rounded-md border px-3 py-2 transition-colors',
                  'flex items-center gap-2',
                  relevanceBlockClass(object.relevance),
                )}
              >
                <ChevronRight className="h-4 w-4 shrink-0 theme-text-secondary transition-transform group-open:rotate-90" />
                <span className={cn('text-[12px] font-mono', relevanceClass(object.relevance))}>
                  {[object.catalog, object.schema, object.objectName].filter(Boolean).join('.')}
                </span>
              </summary>
              <div className="mt-2">
                <SqlCodeBlock
                  variant="block"
                  language="sql"
                  sql={object.objectDdl}
                  wrapLongLines={true}
                />
              </div>
            </details>
          ) : (
            <div className={cn('rounded-md border px-3 py-2', relevanceBlockClass(object.relevance))}>
              <p className={cn('text-[12px] font-mono', relevanceClass(object.relevance))}>
                {[object.catalog, object.schema, object.objectName].filter(Boolean).join('.')}
              </p>
            </div>
          )}
        </div>
      ))}
      {result.rawResponse && rawResponseNode}
    </div>
  );
}

export function SubAgentConsole({ metadata }: SubAgentConsoleProps) {
  const markdownComponents = useMarkdownComponents();
  const invocation = metadata.invocations[0];
  const agentType = invocation?.agentType ?? metadata.agentType;
  const isExplorer = agentType === 'explorer';
  const AgentIcon = isExplorer ? Database : Braces;
  const accentColor = isExplorer ? 'text-cyan-600 dark:text-cyan-400' : 'text-purple-600 dark:text-purple-400';
  const title = invocation?.taskLabel ?? SUB_AGENT_LABELS[agentType] ?? agentType;
  const status = invocation?.status ?? metadata.status;
  const statusText = invocation ? getInvocationStatusText(invocation) : 'Starting Agent...';
  const resultSummary = invocation?.resultSummary ?? metadata.summary;
  const resultJson = invocation?.resultJson ?? metadata.resultJson;
  const startedAt = invocation?.startedAt ?? metadata.startedAt;
  const completedAt = invocation?.completedAt ?? metadata.completedAt;

  return (
    <div className="flex-1 overflow-auto theme-bg-main p-6">
      <div className="max-w-4xl mx-auto flex flex-col gap-4">
        <Section title="Overview">
          <div className="flex items-start gap-3">
            <AgentIcon className={cn('w-5 h-5 mt-0.5', accentColor)} />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-[15px] font-semibold theme-text-primary">{title}</span>
                {getStatusIcon(status)}
              </div>
              <p className="mt-2 text-[12px] theme-text-primary">{statusText}</p>
            </div>
          </div>
        </Section>

        <Section title="Tool Calls">
          <ToolCalls invocation={invocation} />
        </Section>

        {(resultSummary || resultJson) && (
          <Section title="Result">
            {resultSummary && (
              <div className="text-[12px] theme-text-primary">
                <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
                  {resultSummary}
                </ReactMarkdown>
              </div>
            )}
            {isExplorer ? (
              <ExplorerResultPreview
                resultJson={resultJson}
                rawResponseNode={resultJson ? (
                  <div className="text-[12px] theme-text-primary">
                    <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
                      {parseExplorerResult(resultJson).rawResponse ?? ''}
                    </ReactMarkdown>
                  </div>
                ) : undefined}
              />
            ) : resultJson && (
              <details className="mt-3 rounded-md theme-bg-tertiary px-3 py-2">
                <summary className="cursor-pointer text-[11px] theme-text-secondary">Show Raw JSON</summary>
                <pre className="mt-2 text-[10px] font-mono whitespace-pre-wrap break-words theme-text-primary">
                  {formatJson(resultJson)}
                </pre>
              </details>
            )}
          </Section>
        )}
      </div>
    </div>
  );
}
