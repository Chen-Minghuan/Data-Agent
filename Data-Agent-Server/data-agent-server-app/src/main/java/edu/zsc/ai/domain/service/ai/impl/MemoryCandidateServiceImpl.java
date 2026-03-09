package edu.zsc.ai.domain.service.ai.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.ai.AiMemoryCandidateMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCandidateServiceImpl extends ServiceImpl<AiMemoryCandidateMapper, AiMemoryCandidate>
        implements MemoryCandidateService {

    private static final int MAX_LIST_LIMIT = 100;

    private final AiConversationService aiConversationService;
    private final MemoryService memoryService;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<AiMemoryCandidate> listCurrentConversationCandidates(Long conversationId, int limit) {
        Long userId = requireUserId();
        checkConversationAccess(userId, conversationId);

        int safeLimit = Math.max(1, Math.min(limit, MAX_LIST_LIMIT));
        LambdaQueryWrapper<AiMemoryCandidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemoryCandidate::getUserId, userId)
                .eq(AiMemoryCandidate::getConversationId, conversationId)
                .orderByAsc(AiMemoryCandidate::getCreatedAt)
                .orderByAsc(AiMemoryCandidate::getId);
        Page<AiMemoryCandidate> page = new Page<>(1, safeLimit, false);
        return page(page, wrapper).getRecords();
    }

    @Override
    public AiMemoryCandidate createCandidate(Long conversationId,
                                             String candidateType,
                                             String candidateContent,
                                             String reason) {
        Long userId = requireUserId();
        checkConversationAccess(userId, conversationId);

        MemoryTypeEnum type = MemoryTypeEnum.fromValue(candidateType);
        String content = StringUtils.trimToEmpty(candidateContent);
        BusinessException.assertFalse(content.isEmpty(), "Candidate content cannot be blank");

        LocalDateTime now = LocalDateTime.now();
        AiMemoryCandidate candidate = AiMemoryCandidate.builder()
                .userId(userId)
                .conversationId(conversationId)
                .candidateType(type.name())
                .candidateContent(content)
                .reason(StringUtils.trimToNull(reason))
                .createdAt(now)
                .updatedAt(now)
                .build();

        save(candidate);
        return candidate;
    }

    @Override
    public boolean deleteCandidate(Long candidateId) {
        Long userId = requireUserId();
        if (Objects.isNull(candidateId)) {
            return false;
        }

        AiMemoryCandidate candidate = getById(candidateId);
        if (Objects.isNull(candidate)) {
            return false;
        }

        BusinessException.assertTrue(userId.equals(candidate.getUserId()), "error.forbidden");
        return removeById(candidateId);
    }

    @Override
    public List<AiMemory> commitCandidates(Long conversationId, List<Long> candidateIds) {
        Long userId = requireUserId();
        checkConversationAccess(userId, conversationId);
        BusinessException.assertFalse(CollectionUtils.isEmpty(candidateIds), "Candidate ids cannot be empty");

        LambdaQueryWrapper<AiMemoryCandidate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemoryCandidate::getUserId, userId)
                .eq(AiMemoryCandidate::getConversationId, conversationId)
                .in(AiMemoryCandidate::getId, candidateIds)
                .orderByAsc(AiMemoryCandidate::getCreatedAt)
                .orderByAsc(AiMemoryCandidate::getId);

        List<AiMemoryCandidate> candidates = list(wrapper);
        BusinessException.assertTrue(candidates.size() == candidateIds.size(), "error.forbidden");

        // Pre-compute embeddings outside transaction to avoid long-held DB connections
        List<Embedding> embeddings = new ArrayList<>(candidates.size());
        for (AiMemoryCandidate candidate : candidates) {
            String content = StringUtils.trimToEmpty(candidate.getCandidateContent());
            embeddings.add(embeddingModel.embed(content).content());
        }

        return doCommitInTransaction(candidates, embeddings);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<AiMemory> doCommitInTransaction(List<AiMemoryCandidate> candidates, List<Embedding> embeddings) {
        List<AiMemory> created = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            created.add(memoryService.createFromCandidateWithEmbedding(candidates.get(i), embeddings.get(i)));
        }
        removeBatchByIds(candidates.stream().map(AiMemoryCandidate::getId).toList());
        return created;
    }

    private Long requireUserId() {
        Long userId = RequestContext.getUserId();
        BusinessException.assertNotNull(userId, "error.not.login");
        return userId;
    }

    private void checkConversationAccess(Long userId, Long conversationId) {
        BusinessException.assertNotNull(conversationId, "Conversation id is required");
        aiConversationService.checkAccess(userId, conversationId);
    }
}
