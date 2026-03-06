package edu.zsc.ai.config.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import edu.zsc.ai.agent.tool.annotation.AgentTool;

import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.common.enums.ai.PromptLanguageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures multiple StreamingChatModel beans per supported model (ModelEnum),
 * and provides ReActAgentProvider for runtime selection by request model/language.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(QwenProperties.class)
public class MultiModelAgentConfig {

    /**
     * Static prompt cache: loaded once at class initialization, then reused by all agent beans.
     */
    private static final Map<PromptLanguageEnum, String> SYSTEM_PROMPTS = Map.of(
            PromptLanguageEnum.EN, loadSystemPrompt(PromptLanguageEnum.EN.getSystemPromptResource()),
            PromptLanguageEnum.ZH, loadSystemPrompt(PromptLanguageEnum.ZH.getSystemPromptResource())
    );

    private final QwenProperties qwenProperties;

    @Bean("agentTools")
    public List<Object> agentTools(ApplicationContext context) {
        return new ArrayList<>(context.getBeansWithAnnotation(AgentTool.class).values());
    }

    @Bean("streamingChatModelQwen3Max")
    public StreamingChatModel streamingChatModelQwen3Max() {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    @Bean("streamingChatModelQwen3MaxThinking")
    public StreamingChatModel streamingChatModelQwen3MaxThinking() {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(true)
                                .thinkingBudget(qwenProperties.getParameters().getThinkingBudget())
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    @Bean("streamingChatModelQwenPlus")
    public StreamingChatModel streamingChatModelQwenPlus() {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(ModelEnum.QWEN_PLUS.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    private ReActAgent buildAgent(StreamingChatModel streamingChatModel,
                                  ChatMemoryProvider chatMemoryProvider,
                                  List<Object> agentTools,
                                  String systemPrompt) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .systemMessage(systemPrompt)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(agentTools)
                .build();
    }

    @Bean
    @Primary
    public ReActAgentProvider reActAgentProvider(
            @Qualifier("streamingChatModelQwen3Max") StreamingChatModel streamingChatModelQwen3Max,
            @Qualifier("streamingChatModelQwen3MaxThinking") StreamingChatModel streamingChatModelQwen3MaxThinking,
            @Qualifier("streamingChatModelQwenPlus") StreamingChatModel streamingChatModelQwenPlus,
            ChatMemoryProvider chatMemoryProvider,
            @Qualifier("agentTools") List<Object> agentTools) {
        Map<String, StreamingChatModel> modelsByName = Map.of(
                ModelEnum.QWEN3_MAX.getModelName(), streamingChatModelQwen3Max,
                ModelEnum.QWEN3_MAX_THINKING.getModelName(), streamingChatModelQwen3MaxThinking,
                ModelEnum.QWEN_PLUS.getModelName(), streamingChatModelQwenPlus
        );
        Map<String, ReActAgent> dynamicAgentCache = new ConcurrentHashMap<>();

        return (modelName, language) -> {
            PromptLanguageEnum promptLanguage = PromptLanguageEnum.fromRequestLanguage(language);
            StreamingChatModel model = modelsByName.get(modelName);
            if (model == null) {
                throw new IllegalArgumentException(
                        "No StreamingChatModel configured for model=" + modelName + ", language=" + promptLanguage.getCode());
            }
            String cacheKey = modelName + "::" + promptLanguage.getCode();
            return dynamicAgentCache.computeIfAbsent(cacheKey, key -> {
                log.info("Create ReActAgent dynamically: model={}, language={}", modelName, promptLanguage.getCode());
                return buildAgent(model, chatMemoryProvider, agentTools, systemPrompt(promptLanguage));
            });
        };
    }

    private static String systemPrompt(PromptLanguageEnum language) {
        return SYSTEM_PROMPTS.get(language);
    }

    private static String loadSystemPrompt(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load system prompt resource: " + resourcePath, e);
        }
    }
}
