package edu.zsc.ai.agent.subagent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubAgentMemoryTest {

    @Test
    void createTemporaryMemory_isIndependent() {
        ChatMemory memory1 = SubAgentMemoryFactory.createTemporary();
        ChatMemory memory2 = SubAgentMemoryFactory.createTemporary();

        memory1.add(UserMessage.from("hello from memory1"));

        // memory2 should be completely independent
        assertEquals(1, memory1.messages().size());
        assertEquals(0, memory2.messages().size());
    }

    @Test
    void createTemporaryMemory_respectsMaxMessages() {
        ChatMemory memory = SubAgentMemoryFactory.createTemporary(3);

        memory.add(UserMessage.from("msg1"));
        memory.add(UserMessage.from("msg2"));
        memory.add(UserMessage.from("msg3"));
        memory.add(UserMessage.from("msg4"));

        // Window should keep only the last 3
        assertEquals(3, memory.messages().size());
        assertEquals("msg2", ((UserMessage) memory.messages().get(0)).singleText());
    }

    @Test
    void createTemporaryMemory_defaultWindowSize() {
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();

        // Add more messages than the default window — excess should be evicted
        for (int i = 0; i < SubAgentMemoryFactory.DEFAULT_WINDOW_SIZE + 20; i++) {
            memory.add(UserMessage.from("msg" + i));
        }

        // Should be capped at default window size
        assertEquals(SubAgentMemoryFactory.DEFAULT_WINDOW_SIZE, memory.messages().size());
    }

    @Test
    void buildContext_instructionOnly() {
        String result = SubAgentContextBuilder.builder()
                .instruction("show me all orders")
                .connectionId(1L)
                .build();

        assertTrue(result.contains("show me all orders"));
        assertTrue(result.contains("1"));
    }

    @Test
    void buildContext_withContext() {
        String result = SubAgentContextBuilder.builder()
                .instruction("filter by last month")
                .connectionId(1L)
                .context("User was exploring the orders table. Previous query returned 500 rows.")
                .build();

        assertTrue(result.contains("filter by last month"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("orders table"));
    }

    @Test
    void buildContext_withErrorInContext() {
        String result = SubAgentContextBuilder.builder()
                .instruction("show me all orders")
                .connectionId(1L)
                .context("Previous error: column \"amount\" does not exist")
                .build();

        assertTrue(result.contains("show me all orders"));
        assertTrue(result.contains("amount"));
        assertTrue(result.contains("does not exist"));
    }
}
