package edu.zsc.ai.domain.service.ai;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;

public interface MemoryCandidateService extends IService<AiMemoryCandidate> {

    List<AiMemoryCandidate> listCurrentConversationCandidates(Long conversationId, int limit);

    AiMemoryCandidate createCandidate(Long conversationId,
                                      String candidateType,
                                      String candidateContent,
                                      String reason);

    boolean deleteCandidate(Long candidateId);

    List<AiMemory> commitCandidates(Long conversationId, List<Long> candidateIds);
}
