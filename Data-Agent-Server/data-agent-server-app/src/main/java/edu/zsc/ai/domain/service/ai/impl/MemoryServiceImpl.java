package edu.zsc.ai.domain.service.ai.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import edu.zsc.ai.common.enums.ai.MemoryStatusEnum;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.ai.AiMemoryMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemoryServiceImpl extends ServiceImpl<AiMemoryMapper, AiMemory> implements MemoryService {

    private static final int MAX_SEARCH_LIMIT = 30;
    private static final int ACTIVE_MEMORY_STATUS = MemoryStatusEnum.ACTIVE.getCode();

    private static final String METADATA_KEY_USER_ID = "userId";
    private static final String METADATA_KEY_STATUS = "status";
    private static final String METADATA_KEY_MEMORY_TYPE = "memoryType";
    private static final String METADATA_KEY_CONVERSATION_ID = "conversationId";
    private static final String METADATA_KEY_MEMORY_ID = "memoryId";

    private static final String ERR_USER_AND_CANDIDATE_REQUIRED = "User id and candidate are required";
    private static final String ERR_CANDIDATE_CONTENT_EMPTY = "Candidate content cannot be empty";
    private static final String ERR_EMBEDDING_EMPTY = "Embedding cannot be empty";

    private final EmbeddingStore<TextSegment> memoryEmbeddingStore;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<MemorySearchResult> searchActiveMemories(Long userId, String queryText, int limit, double minScore) {
        if (Objects.isNull(userId) || StringUtils.isBlank(queryText)) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_LIMIT));

        Embedding queryEmbedding = embeddingModel.embed(queryText).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(safeLimit)
                .minScore(minScore)
                .filter(MetadataFilterBuilder.metadataKey(METADATA_KEY_USER_ID).isEqualTo(userId)
                        .and(MetadataFilterBuilder.metadataKey(METADATA_KEY_STATUS).isEqualTo(ACTIVE_MEMORY_STATUS)))
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = memoryEmbeddingStore.search(request);

        return searchResult.matches().stream()
                .map(this::toMemorySearchResult)
                .toList();
    }

    @Override
    public AiMemory createFromCandidate(Long userId, Long conversationId, AiMemoryCandidate candidate) {
        if (Objects.isNull(userId) || Objects.isNull(candidate)) {
            throw BusinessException.badRequest(ERR_USER_AND_CANDIDATE_REQUIRED);
        }
        String content = StringUtils.trimToEmpty(candidate.getCandidateContent());
        if (content.isEmpty()) {
            throw BusinessException.badRequest(ERR_CANDIDATE_CONTENT_EMPTY);
        }

        Embedding embedding = embeddingModel.embed(content).content();
        return createFromCandidateWithEmbedding(userId, conversationId, candidate, embedding);
    }

    @Override
    public AiMemory createFromCandidateWithEmbedding(Long userId, Long conversationId,
                                                      AiMemoryCandidate candidate, Embedding embedding) {
        if (Objects.isNull(userId) || Objects.isNull(candidate)) {
            throw BusinessException.badRequest(ERR_USER_AND_CANDIDATE_REQUIRED);
        }
        String content = StringUtils.trimToEmpty(candidate.getCandidateContent());
        if (content.isEmpty()) {
            throw BusinessException.badRequest(ERR_CANDIDATE_CONTENT_EMPTY);
        }
        if (Objects.isNull(embedding) || Objects.isNull(embedding.vector())) {
            throw BusinessException.badRequest(ERR_EMBEDDING_EMPTY);
        }

        LocalDateTime now = LocalDateTime.now();

        AiMemory memory = AiMemory.builder()
                .userId(userId)
                .status(ACTIVE_MEMORY_STATUS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        save(memory);

        long resolvedConversationId = Objects.requireNonNullElse(conversationId, candidate.getConversationId());
        Metadata metadata = new Metadata()
                .put(METADATA_KEY_USER_ID, userId)
                .put(METADATA_KEY_STATUS, ACTIVE_MEMORY_STATUS)
                .put(METADATA_KEY_MEMORY_TYPE, candidate.getCandidateType())
                .put(METADATA_KEY_CONVERSATION_ID, resolvedConversationId)
                .put(METADATA_KEY_MEMORY_ID, memory.getId());
        TextSegment textSegment = TextSegment.from(content, metadata);

        memoryEmbeddingStore.addAll(
                List.of(String.valueOf(memory.getId())),
                List.of(embedding),
                List.of(textSegment));

        return memory;
    }

    private MemorySearchResult toMemorySearchResult(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();

        return MemorySearchResult.builder()
                .id(metadata.getLong(METADATA_KEY_MEMORY_ID))
                .memoryType(metadata.getString(METADATA_KEY_MEMORY_TYPE))
                .content(segment.text())
                .score(match.score())
                .conversationId(metadata.getLong(METADATA_KEY_CONVERSATION_ID))
                .build();
    }
}
