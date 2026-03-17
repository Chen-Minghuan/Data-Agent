import { Braces, CheckCircle, Database, Loader2, XCircle, Zap } from 'lucide-react';
import { cn } from '../../../lib/utils';
import { getAgentTheming } from './subAgentDataHelpers';

export interface SingleSubAgentCardProps {
  agentType: string;
  label: string;
  statusText: string;
  isComplete: boolean;
  isError: boolean;
  elapsedText?: string;
  onViewConsole?: () => void;
}

export function SingleSubAgentCard({
  agentType,
  label,
  statusText,
  isComplete,
  isError,
  elapsedText,
  onViewConsole,
}: SingleSubAgentCardProps) {
  const { isExplorer, borderColor, bgColor, iconColor } = getAgentTheming(agentType, isError);
  const AgentIcon = isExplorer ? Database : Braces;

  return (
    <div className={cn('mb-2 rounded-lg border overflow-hidden', borderColor, bgColor)}>
      <div className="px-3 py-2 flex items-center gap-2">
        {isComplete && !isError ? (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" />
        ) : isError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" />
        ) : (
          <Loader2 className={cn('w-3.5 h-3.5 shrink-0 animate-spin', iconColor)} />
        )}
        <AgentIcon className={cn('w-3.5 h-3.5 shrink-0', iconColor)} />
        <span className="text-[12px] font-medium theme-text-primary">{label}</span>
        <span className="ml-auto flex items-center gap-3 text-[11px] theme-text-secondary tabular-nums">
          {elapsedText && <span>{elapsedText}</span>}
          {onViewConsole && (
            <button
              type="button"
              onClick={onViewConsole}
              className={cn(
                'hover:underline shrink-0 flex items-center gap-1',
                isExplorer ? 'text-cyan-600 dark:text-cyan-400' : 'text-purple-600 dark:text-purple-400'
              )}
            >
              <Zap className="w-3 h-3" />
              View Console
            </button>
          )}
        </span>
      </div>
      <div className="px-3 pb-2 -mt-0.5">
        <p className={cn('text-[11px] truncate pl-5', isComplete ? 'theme-text-primary' : 'theme-text-secondary')}>
          {statusText}
        </p>
      </div>
    </div>
  );
}
