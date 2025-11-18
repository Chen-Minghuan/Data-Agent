-- ===============================================
-- ai_conversation Table
-- ===============================================

-- Table DDL
CREATE TABLE ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    delete_flag SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table and column comments
COMMENT ON TABLE ai_conversation IS 'AI assistant conversation table';
COMMENT ON COLUMN ai_conversation.id IS 'Primary key ID for conversation';
COMMENT ON COLUMN ai_conversation.user_id IS 'Associated user ID';
COMMENT ON COLUMN ai_conversation.title IS 'Conversation title, can be generated from first message or customized by user';
COMMENT ON COLUMN ai_conversation.delete_flag IS 'Soft delete flag, 0: normal 1: deleted';
COMMENT ON COLUMN ai_conversation.created_at IS 'Created time';
COMMENT ON COLUMN ai_conversation.updated_at IS 'Updated time';

-- Table indexes
CREATE INDEX idx_ai_conversation_user_id ON ai_conversation(user_id);

-- ===============================================
-- ai_message Table
-- ===============================================

-- Table DDL
CREATE TABLE ai_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    token_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table and column comments
COMMENT ON TABLE ai_message IS 'AI assistant message table';
COMMENT ON COLUMN ai_message.id IS 'Primary key ID for message';
COMMENT ON COLUMN ai_message.conversation_id IS 'Belonged conversation ID';
COMMENT ON COLUMN ai_message.role IS 'Message role, user: user message assistant: AI assistant message';
COMMENT ON COLUMN ai_message.content IS 'Message content';
COMMENT ON COLUMN ai_message.token_count IS 'Token usage statistics';
COMMENT ON COLUMN ai_message.created_at IS 'Created time';
COMMENT ON COLUMN ai_message.updated_at IS 'Updated time';

-- Table indexes
CREATE INDEX idx_ai_message_conversation_id ON ai_message(conversation_id);
CREATE INDEX idx_ai_message_conversation_created ON ai_message(conversation_id, created_at);
CREATE INDEX idx_ai_message_role ON ai_message(role);

-- ===============================================
-- ai_message_block Table
-- ===============================================

-- Table DDL
CREATE TABLE ai_message_block (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    block_type VARCHAR(20) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table and column comments
COMMENT ON TABLE ai_message_block IS 'AI assistant message block table';
COMMENT ON COLUMN ai_message_block.id IS 'Primary key ID for message block';
COMMENT ON COLUMN ai_message_block.message_id IS 'Belonged message ID';
COMMENT ON COLUMN ai_message_block.block_type IS 'Block type, text: text tool_call: tool call tool_result: tool result';
COMMENT ON COLUMN ai_message_block.content IS 'Block content';
COMMENT ON COLUMN ai_message_block.created_at IS 'Created time';
COMMENT ON COLUMN ai_message_block.updated_at IS 'Updated time';

-- Table indexes
CREATE INDEX idx_ai_message_block_message_id ON ai_message_block(message_id);
CREATE INDEX idx_ai_message_block_message_type ON ai_message_block(message_id, block_type);
