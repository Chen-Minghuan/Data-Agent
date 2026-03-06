package edu.zsc.ai.domain.service.ai;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import dev.langchain4j.data.embedding.Embedding;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

public interface MemoryService extends IService<AiMemory> {

    List<MemorySearchResult> searchActiveMemories(Long userId, String queryText, int limit, double minScore);

    AiMemory createFromCandidate(Long userId, Long conversationId, AiMemoryCandidate candidate);

    /**
     * Creates memory from candidate using a pre-computed embedding.
     * Use this to avoid calling the embedding API inside a transaction.
     */
    AiMemory createFromCandidateWithEmbedding(Long userId, Long conversationId, AiMemoryCandidate candidate, Embedding embedding);
}
