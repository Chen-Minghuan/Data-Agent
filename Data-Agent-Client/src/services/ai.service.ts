import http from '../lib/http';
import type { ModelOption } from '../types/ai';

let modelsCache: ModelOption[] | null = null;
let modelsInFlight: Promise<ModelOption[]> | null = null;

export const aiService = {
  /**
   * Get available chat models (e.g. for model selector).
   * Falls back to empty array on error; caller may use a default list.
   */
  getModels: async (): Promise<ModelOption[]> => {
    if (modelsCache) {
      return modelsCache;
    }

    if (modelsInFlight) {
      return modelsInFlight;
    }

    modelsInFlight = (async () => {
      try {
        const response = await http.get<ModelOption[]>('/ai/models');
        const list = Array.isArray(response.data) ? response.data : [];
        const normalized = list.map((m) => ({
          modelName: m.modelName ?? '',
          supportThinking: Boolean(m.supportThinking),
        }));
        if (normalized.length > 0) {
          modelsCache = normalized;
        }
        return normalized;
      } catch {
        return [];
      } finally {
        modelsInFlight = null;
      }
    })();

    return modelsInFlight;
  },
};
