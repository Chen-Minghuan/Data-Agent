package edu.zsc.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import edu.zsc.ai.model.dto.request.CreateConversationRequest;
import edu.zsc.ai.model.dto.response.ApiResponse;
import edu.zsc.ai.model.dto.response.ConversationResponse;
import edu.zsc.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Controller for AI conversation operations
 *
 * @author zgq
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
public class AiConversationController {

    private final AiConversationService aiConversationService;

    /**
     * Create a new conversation
     *
     * @param request conversation creation request
     * @return created conversation response
     */
    @PostMapping("/conversation/create")
    @SaCheckLogin
    public ApiResponse<ConversationResponse> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        log.info("Creating conversation with title: {}", request.getTitle());

        ConversationResponse response = aiConversationService.createConversation(request);

        return ApiResponse.success(response);
    }
}