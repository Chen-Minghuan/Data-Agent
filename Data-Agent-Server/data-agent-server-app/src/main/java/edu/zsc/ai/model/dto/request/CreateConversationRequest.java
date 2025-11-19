package edu.zsc.ai.model.dto.request;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Request DTO for creating conversation
 *
 * @author zgq
 */
@Data
public class CreateConversationRequest {

    /**
     * Conversation title, can be empty and will be generated from first message
     */
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;
}