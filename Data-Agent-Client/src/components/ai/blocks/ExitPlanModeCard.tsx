import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ListTodo, AlertTriangle, Play, X, RotateCcw } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { ExitPlanPayload } from './exitPlanModeTypes';
import { useAIAssistantContext } from '../AIAssistantContext';

export interface ExitPlanModeCardProps {
  payload: ExitPlanPayload;
  submittedAction?: string;
}

export function ExitPlanModeCard({ payload, submittedAction }: ExitPlanModeCardProps) {
  const { t } = useTranslation();
  const { submitMessage, isLoading, agentState } = useAIAssistantContext();
  const [isSubmitted, setIsSubmitted] = useState(false);

  if (submittedAction || isSubmitted) {
    return null;
  }

  const handleExecute = () => {
    if (isLoading) return;
    setIsSubmitted(true);
    agentState.setAgent('Agent');
    // Pass agentType override since React state update (setAgent) hasn't flushed yet
    submitMessage(t('ai.plan.executeMessage', 'Please execute the plan above.'), { agentType: 'Agent' });
  };

  const handleExit = () => {
    if (isLoading) return;
    setIsSubmitted(true);
    agentState.setAgent('Agent');
  };

  const handleContinue = () => {
    if (isLoading) return;
    setIsSubmitted(true);
    submitMessage(t('ai.plan.continueMessage', 'I have additional requirements for this plan.'));
  };

  return (
    <div className="mb-2 p-4 rounded-lg border border-amber-300 dark:border-amber-700 bg-amber-50/50 dark:bg-amber-900/10 shadow-sm flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-medium">
        <ListTodo className="w-4 h-4" />
        <span className="text-[13px] font-semibold">{payload.title}</span>
      </div>

      {/* Steps */}
      <div className="flex flex-col gap-2">
        {payload.steps.map((step) => (
          <div key={step.order} className="rounded border theme-border theme-bg-main overflow-hidden">
            <div className="theme-bg-panel px-3 py-1.5 text-[12px] font-medium border-b theme-border flex justify-between items-center">
              <span>
                <span className="text-amber-600 dark:text-amber-400 font-mono mr-1.5">#{step.order}</span>
                {step.description}
              </span>
              {step.objectName && (
                <span className="opacity-60 font-mono text-[11px]">{step.objectName}</span>
              )}
            </div>
            {step.sql && (
              <div className="p-0 overflow-x-auto text-[12px]">
                <SyntaxHighlighter
                  language="sql"
                  style={vscDarkPlus}
                  customStyle={{
                    margin: 0,
                    padding: '0.75rem',
                    background: 'transparent',
                    fontSize: '12px',
                    textShadow: 'none',
                    fontFamily:
                      'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
                  }}
                  wrapLongLines={true}
                >
                  {step.sql}
                </SyntaxHighlighter>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Risks */}
      {payload.risks.length > 0 && (
        <div className="rounded border border-red-200 dark:border-red-800 bg-red-50/50 dark:bg-red-900/10 p-3">
          <div className="flex items-center gap-1.5 text-red-600 dark:text-red-400 text-[11px] font-medium mb-1.5">
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>{t('ai.plan.risks', 'Risks & Warnings')}</span>
          </div>
          <ul className="list-disc list-inside text-[12px] theme-text-secondary space-y-0.5">
            {payload.risks.map((risk, i) => (
              <li key={i}>{risk}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex gap-2 mt-1">
        <button
          type="button"
          onClick={handleExecute}
          disabled={isLoading}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-[12px] font-medium bg-green-600 hover:bg-green-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Play className="w-3.5 h-3.5" />
          {t('ai.plan.executeBtn', 'Execute Plan')}
        </button>
        <button
          type="button"
          onClick={handleExit}
          disabled={isLoading}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-[12px] font-medium bg-zinc-500 hover:bg-zinc-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <X className="w-3.5 h-3.5" />
          {t('ai.plan.exitBtn', 'Exit')}
        </button>
        <button
          type="button"
          onClick={handleContinue}
          disabled={isLoading}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-[12px] font-medium bg-amber-500 hover:bg-amber-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          {t('ai.plan.continueBtn', 'Continue Planning')}
        </button>
      </div>
    </div>
  );
}
