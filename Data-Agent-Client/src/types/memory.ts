export type MemoryType =
  | 'PREFERENCE'
  | 'BUSINESS_RULE'
  | 'KNOWLEDGE_POINT'
  | 'GOLDEN_SQL_CASE'
  | 'WORKFLOW_CONSTRAINT';

export interface MemoryCandidate {
  id: number;
  conversationId: number;
  candidateType: MemoryType;
  candidateContent: string;
  reason?: string | null;
  createdAt: string;
}

export interface Memory {
  id: number;
  memoryType: MemoryType;
  title: string;
  content: string;
  detailJson: string;
  status: number;
  conversationId?: number | null;
  createdAt: string;
}
