import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronRight, Save, Trash2 } from 'lucide-react';
import { memoryCandidateService } from '../../services/memoryCandidate.service';
import type { MemoryCandidate } from '../../types/memory';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';

interface MemoryCandidateDockProps {
  conversationId: number | null;
  refreshKey: string | number;
}

export function MemoryCandidateDock({ conversationId, refreshKey }: MemoryCandidateDockProps) {
  const { t } = useTranslation();
  const toast = useToast();

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [candidates, setCandidates] = useState<MemoryCandidate[]>([]);
  const [expanded, setExpanded] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const hadCandidatesRef = useRef(false);

  const hasCandidates = candidates.length > 0;

  const loadCandidates = useCallback(async () => {
    if (!conversationId) {
      setCandidates([]);
      setSelectedIds(new Set());
      return;
    }

    setLoading(true);
    try {
      const list = await memoryCandidateService.listCurrentConversation(conversationId);
      setCandidates(list);
      setSelectedIds((prev) => {
        const next = new Set<number>();
        for (const id of prev) {
          if (list.some((item) => item.id === id)) {
            next.add(id);
          }
        }
        return next;
      });
    } catch {
      toast.error(t(I18N_KEYS.AI.MEMORY_CANDIDATE.LOAD_FAILED));
    } finally {
      setLoading(false);
    }
  }, [conversationId, t, toast]);

  useEffect(() => {
    loadCandidates();
  }, [loadCandidates, refreshKey]);

  useEffect(() => {
    // Keep the dock collapsed when there is no candidate, and only auto-expand
    // when candidates appear from an empty state.
    if (!hasCandidates) {
      setExpanded(false);
      hadCandidatesRef.current = false;
      return;
    }
    if (!hadCandidatesRef.current) {
      setExpanded(true);
    }
    hadCandidatesRef.current = true;
  }, [hasCandidates]);

  const toggleSelected = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const allSelected = useMemo(
    () => hasCandidates && selectedIds.size === candidates.length,
    [hasCandidates, selectedIds.size, candidates.length]
  );

  const toggleSelectAll = () => {
    if (!hasCandidates) return;
    if (allSelected) {
      setSelectedIds(new Set());
      return;
    }
    setSelectedIds(new Set(candidates.map((item) => item.id)));
  };

  const handleDeleteOne = async (id: number) => {
    setSubmitting(true);
    try {
      await memoryCandidateService.deleteCandidate(id);
      toast.success(t(I18N_KEYS.AI.MEMORY_CANDIDATE.DELETE_SUCCESS));
      await loadCandidates();
    } catch {
      toast.error(t(I18N_KEYS.AI.MEMORY_CANDIDATE.DELETE_FAILED));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCommitSelected = async () => {
    if (!conversationId || selectedIds.size === 0) return;

    setSubmitting(true);
    try {
      await memoryCandidateService.commitCandidates(conversationId, Array.from(selectedIds));
      toast.success(t(I18N_KEYS.AI.MEMORY_CANDIDATE.COMMIT_SUCCESS));
      setSelectedIds(new Set());
      await loadCandidates();
    } catch {
      toast.error(t(I18N_KEYS.AI.MEMORY_CANDIDATE.COMMIT_FAILED));
    } finally {
      setSubmitting(false);
    }
  };

  if (!conversationId) return null;

  return (
    <div className="shrink-0 border-t theme-border theme-bg-panel">
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        className="w-full px-3 py-2 text-left text-xs theme-text-secondary hover:theme-text-primary transition-colors flex items-center gap-2"
      >
        {expanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
        <span className="font-medium">
          {t(I18N_KEYS.AI.MEMORY_CANDIDATE.TITLE)}
          {hasCandidates ? ` (${candidates.length})` : ''}
        </span>
      </button>

      {expanded && (
        <div className="px-3 pb-3 space-y-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={toggleSelectAll}
              disabled={!hasCandidates || submitting}
              className="text-[11px] px-2 py-1 rounded border theme-border theme-text-secondary hover:theme-text-primary"
            >
              {allSelected
                ? t(I18N_KEYS.AI.MEMORY_CANDIDATE.UNSELECT_ALL)
                : t(I18N_KEYS.AI.MEMORY_CANDIDATE.SELECT_ALL)}
            </button>
            <button
              type="button"
              onClick={handleCommitSelected}
              disabled={selectedIds.size === 0 || submitting}
              className="text-[11px] px-2 py-1 rounded bg-emerald-600 text-white disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-1"
            >
              <Save className="w-3 h-3" />
              {t(I18N_KEYS.AI.MEMORY_CANDIDATE.COMMIT_SELECTED)}
            </button>
          </div>

          <div className="space-y-2 max-h-44 overflow-y-auto pr-1">
            {loading ? (
              <p className="text-xs theme-text-secondary">{t(I18N_KEYS.AI.MEMORY_CANDIDATE.LOADING)}</p>
            ) : !hasCandidates ? (
              <p className="text-xs theme-text-secondary">{t(I18N_KEYS.AI.MEMORY_CANDIDATE.EMPTY)}</p>
            ) : (
              candidates.map((candidate) => (
                <div key={candidate.id} className="p-2 rounded border theme-border theme-bg-main">
                  <div className="flex items-start gap-2">
                    <input
                      type="checkbox"
                      checked={selectedIds.has(candidate.id)}
                      onChange={() => toggleSelected(candidate.id)}
                      className="mt-0.5"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 text-[10px] uppercase theme-text-secondary mb-1">
                        <span>{candidate.candidateType}</span>
                      </div>
                      <p className="text-xs theme-text-primary whitespace-pre-wrap">{candidate.candidateContent}</p>
                      {candidate.reason && (
                        <p className="text-[11px] theme-text-secondary mt-1">{candidate.reason}</p>
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => handleDeleteOne(candidate.id)}
                      disabled={submitting}
                      className="p-1 rounded theme-text-secondary hover:text-red-500"
                      aria-label={t(I18N_KEYS.AI.MEMORY_CANDIDATE.DELETE)}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
