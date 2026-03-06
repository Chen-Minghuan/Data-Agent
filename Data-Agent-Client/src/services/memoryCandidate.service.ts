import http from '../lib/http';
import { MemoryPaths } from '../constants/apiPaths';
import type { Memory, MemoryCandidate } from '../types/memory';

export const memoryCandidateService = {
  listCurrentConversation: async (conversationId: number): Promise<MemoryCandidate[]> => {
    const response = await http.get<MemoryCandidate[]>(MemoryPaths.CANDIDATES_CURRENT_CONVERSATION, {
      params: { conversationId },
    });
    return Array.isArray(response.data) ? response.data : [];
  },

  deleteCandidate: async (id: number): Promise<void> => {
    await http.delete(`${MemoryPaths.CANDIDATES}/${id}`);
  },

  commitCandidates: async (conversationId: number, candidateIds: number[]): Promise<Memory[]> => {
    const response = await http.post<Memory[]>(MemoryPaths.CANDIDATES_COMMIT, {
      conversationId,
      candidateIds,
    });
    return Array.isArray(response.data) ? response.data : [];
  },
};
