package edu.zsc.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.model.entity.AiConversation;
import edu.zsc.ai.model.dto.request.CreateConversationRequest;
import edu.zsc.ai.model.dto.response.ConversationResponse;

/**
 * Service interface for ai_conversation operations
 *
 * @author zgq
 */
public interface AiConversationService extends IService<AiConversation> {

    /**
     * Create a new conversation
     *
     * @param request conversation creation request
     * @return created conversation response
     */
    ConversationResponse createConversation(CreateConversationRequest request);
}