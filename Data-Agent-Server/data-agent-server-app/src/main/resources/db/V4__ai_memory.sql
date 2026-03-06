-- ===============================================
-- AI: Long-term memory + memory candidates
-- Tables: ai_memory, ai_memory_candidate
-- ===============================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_memory (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT NOT NULL,
    status                 SMALLINT NOT NULL DEFAULT 0,
    access_count           INT NOT NULL DEFAULT 0,
    last_accessed_at       TIMESTAMP,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_memory IS 'Long-term memory mutable state table';
COMMENT ON COLUMN ai_memory.user_id IS 'Owner user id';
COMMENT ON COLUMN ai_memory.status IS 'Memory lifecycle status: 0=ACTIVE, 1=ARCHIVED';
COMMENT ON COLUMN ai_memory.access_count IS 'Number of times this memory has been recalled';

CREATE TABLE IF NOT EXISTS ai_memory_candidate (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL,
    conversation_id       BIGINT NOT NULL,
    candidate_type        VARCHAR(32) NOT NULL,
    candidate_content     TEXT NOT NULL,
    reason                TEXT,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_memory_candidate IS 'Candidate memories waiting for user commit';
COMMENT ON COLUMN ai_memory_candidate.user_id IS 'Owner user id';
COMMENT ON COLUMN ai_memory_candidate.conversation_id IS 'Conversation where candidate was created';
COMMENT ON COLUMN ai_memory_candidate.candidate_type IS 'Candidate memory type';
COMMENT ON COLUMN ai_memory_candidate.candidate_content IS 'Candidate memory text';
COMMENT ON COLUMN ai_memory_candidate.reason IS 'Reason why this candidate should be saved';

CREATE INDEX IF NOT EXISTS idx_ai_memory_candidate_user_conv_created
    ON ai_memory_candidate (user_id, conversation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_status
    ON ai_memory (user_id, status);
