package edu.zsc.ai.domain.model.entity.ai;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("ai_memory_candidate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMemoryCandidate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long conversationId;

    private String candidateType;

    private String candidateContent;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
