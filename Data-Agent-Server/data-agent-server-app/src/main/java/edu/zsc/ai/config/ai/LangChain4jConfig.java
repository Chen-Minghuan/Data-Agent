package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.tool.TableTool;
import edu.zsc.ai.tool.TodoTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LangChain4jConfig {

    /** Max number of messages to retain in chat memory (no token counting, avoids DashScope Tokenization API). */
    public static final int MAX_MEMORY_MESSAGES = 50;

    private final ChatMemoryStore chatMemoryStore;

    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MEMORY_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    /**
     * ReActAgent built with AiServices.builder(). Tools run synchronously so that
     * onToolExecuted is invoked and TOOL_RESULT is emitted in the stream.
     */
    @Bean
    @ConditionalOnMissingBean(ReActAgent.class)
    public ReActAgent reActAgent(
            StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool)
                .build();
    }
}
