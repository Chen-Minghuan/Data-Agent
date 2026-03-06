package edu.zsc.ai.api.controller.ai;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.common.converter.ai.MemoryConverter;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.ai.CommitCandidateRequest;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryCandidateResponse;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/memories/candidates")
@RequiredArgsConstructor
public class MemoryCandidateController {

    private static final int DEFAULT_LIST_LIMIT = 100;

    private final MemoryCandidateService memoryCandidateService;

    @GetMapping("/current-conversation")
    public ApiResponse<List<MemoryCandidateResponse>> listCurrentConversation(
            @RequestParam @NotNull Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AiMemoryCandidate> list = memoryCandidateService
                .listCurrentConversationCandidates(userId, conversationId, DEFAULT_LIST_LIMIT);
        return ApiResponse.success(list.stream().map(MemoryConverter::toCandidateResponse).toList());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCandidate(@PathVariable("id") @NotNull Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean deleted = memoryCandidateService.deleteCandidate(userId, id);
        if (!deleted) {
            throw BusinessException.notFound("Candidate not found");
        }
        return ApiResponse.success();
    }

    @PostMapping("/commit")
    public ApiResponse<List<MemoryResponse>> commitCandidates(@Valid @RequestBody CommitCandidateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AiMemory> memories = memoryCandidateService
                .commitCandidates(userId, request.getConversationId(), request.getCandidateIds());
        return ApiResponse.success(memories.stream().map(MemoryConverter::toMemoryResponse).toList());
    }
}
