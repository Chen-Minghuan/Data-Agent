package edu.zsc.ai.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.converter.ConversationConverter;
import edu.zsc.ai.mapper.AiConversationMapper;
import edu.zsc.ai.model.entity.AiConversation;
import edu.zsc.ai.model.dto.request.CreateConversationRequest;
import edu.zsc.ai.model.dto.response.ConversationResponse;
import edu.zsc.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ai_conversation operations
 *
 * @author zgq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    @Override
    public ConversationResponse createConversation(CreateConversationRequest request) {
        // Get current user ID from sa-token
        Long userId = StpUtil.getLoginIdAsLong();

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.getTitle());

        this.save(conversation);

        log.info("Created conversation: id={}, userId={}, title={}", conversation.getId(), userId, request.getTitle());

        return ConversationConverter.toResponse(conversation);
    }
}