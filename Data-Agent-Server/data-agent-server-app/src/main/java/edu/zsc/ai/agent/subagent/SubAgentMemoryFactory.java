package edu.zsc.ai.agent.subagent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * Creates temporary, isolated chat memory for sub-agent invocations.
 * Each call produces an independent in-memory window — no data leaks to MainAgent history.
 */
public final class SubAgentMemoryFactory {

    static final int DEFAULT_WINDOW_SIZE = 100;

    private SubAgentMemoryFactory() {}

    public static ChatMemory createTemporary() {
        return createTemporary(DEFAULT_WINDOW_SIZE);
    }

    public static ChatMemory createTemporary(int maxMessages) {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }
}
