package edu.zsc.ai.service.impl.ai;


import edu.zsc.ai.enums.ai.ChatStatusEnum;
import edu.zsc.ai.enums.ai.ModelEnum;
import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import edu.zsc.ai.model.dto.response.ai.ChatBlockResponse;
import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.service.manager.*;
import edu.zsc.ai.service.parse.ParseManager;
import edu.zsc.ai.service.parse.result.ParseResult;
import edu.zsc.ai.service.parse.rule.ToolUseXmlParseRule;
import edu.zsc.ai.service.tool.ToolRegistry;
import edu.zsc.ai.service.tool.ToolCallResult;
import edu.zsc.ai.util.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class TaskManagerImpl implements TaskManager {

    @Autowired
    private ConversationManager conversationManager;
    @Autowired
    private ContextManager contextManager;
    @Autowired
    private MessageStorage messageStorage;
    @Autowired
    private ToolCallingManager toolCallingManager;
    @Autowired
    private ConcurrencyManager concurrencyManager;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ParseManager parseManager;

    @Autowired
    private ToolRegistry toolRegistry;

    @Override
    public Flux<Object> executeChatTask(ChatRequest request) {
        log.debug("Starting chat task processing, conversationId: {}, message: {}", request.getConversationId(), request.getMessage());

        return Flux.create(sink -> {
            try {
                //1. create or get conversation & check conversation status(if Processing, enqueue chatRequest by ConcurrencyManager)

                // Step 1: Create or get conversation
                Long conversationId = conversationManager.createOrGetConversation(
                        request.getConversationId(),
                        request.getMessage()
                );
                log.debug("Retrieved conversation ID: {}", conversationId);

                // Check concurrency status, try to lock conversation
                if (!concurrencyManager.tryLockConversation(conversationId)) {
                    log.debug("Conversation {} is being processed, adding request to queue", conversationId);

                    // Send queued status to client
                    sink.next(ChatBlockResponse.builder()
                            .conversationId(conversationId)
                            .status(ChatStatusEnum.QUEUED.name())
                            .data("Your request has been added to the queue, please wait...")
                            .build());

                    // Add request to queue
                    concurrencyManager.enqueueMessage(conversationId, request);
                    sink.complete();
                    return;
                }

                log.debug("Successfully locked conversation {}", conversationId);

                // Use try-finally to ensure lock is always released
                try {
                    //2. get history messages
                    log.debug("Getting history messages for conversation {}", conversationId);
                    HistoryContextResponse historyContext = contextManager.getContextForAI(conversationId);
                    log.debug("Retrieved {} history messages", historyContext.getMessages().size());

                    //3. save user message
                    log.debug("Saving user message to conversation {}", conversationId);
                    Long userMessageId = messageStorage.saveUserMessage(conversationId, request.getMessage());
                    log.debug("User message saved with ID: {}", userMessageId);

                    /*4. build context
                     *    4.1 calculate history messages tokens
                     *        4.1.1 if exceed max tokens, compress history messages(but not now)
                     *    4.2 add system prompt
                     *    4.3 add user message
                     *    4.4 add relevant knowledge base contents (but not now)
                     **/
                    ModelEnum model = ModelEnum.findByModelNameOrDefaultModel(request.getModel());
                    log.debug("Using model: {}, max input: {}k, compression threshold: {}k",
                            model.getModelName(), model.getMaxInputTokens(), model.getCompressionThreshold());

                    Integer totalTokenCount = historyContext.getTotalTokenCount();
                    if (totalTokenCount > model.getCompressionThresholdTokenCount()) {
                        log.debug("Token count {} exceeds compression threshold {}, compressing context",
                                totalTokenCount, model.getCompressionThresholdTokenCount());
                        historyContext = contextManager.compressContext(conversationId, historyContext);
                    }
                    ArrayList<Message> messages = new ArrayList<>(historyContext.getMessages().size() + 2);
                    messages.addAll(historyContext.getMessages());
                    messages.add(new SystemMessage(PromptLoader.getSystemPrompt()));
                    messages.add(new UserMessage(request.getMessage()));
                    /*5. call chat model API and handle tool execution
                     *    5.1 execute chat model API call
                     *    5.2 parse model output for tool calls
                     *    5.3 if tool calls found, execute them and feed results back to model
                     *    5.4 continue loop until no more tool calls needed
                     *    5.5 emit final response to client
                     */
                    executeChatLoop(conversationId, messages, sink);

                } finally {
                    // Ensure conversation lock is released
                    concurrencyManager.unlockConversation(conversationId);
                    log.debug("Released conversation lock: {}", conversationId);
                }

            } catch (Exception e) {
                log.error("Exception occurred while processing chat task", e);
                sink.error(e);
            }
        });
    }

    /**
     * Execute chat loop with tool calling support
     */
    private void executeChatLoop(Long conversationId, List<Message> messages, reactor.core.publisher.FluxSink<Object> sink) {
        try {
            log.debug("Starting chat loop for conversation: {}", conversationId);

            // Execute chat model call
            String chatResponse = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            log.debug("Received chat response for conversation {}: {}", conversationId, chatResponse);

            // Parse the response for tool calls
            List<ParseResult> parseResults = parseManager.parse(chatResponse);

            // Check if any tool calls were found
            boolean hasToolCalls = parseResults.stream()
                    .anyMatch(result -> "TOOL_CALL".equals(result.getType()));

            if (!hasToolCalls) {
                // No tool calls, emit the response directly
                ChatBlockResponse response = ChatBlockResponse.builder()
                        .conversationId(conversationId)
                        .status("COMPLETED")
                        .data(chatResponse)
                        .build();
                sink.next(response);
                sink.complete();
                return;
            }

            // Handle tool calls
            handleToolCalls(conversationId, messages, parseResults, sink);

        } catch (Exception e) {
            log.error("Error in chat loop for conversation: {}", conversationId, e);
            sink.error(e);
        }
    }

    /**
     * Handle tool calls from parsed response
     */
    private void handleToolCalls(Long conversationId, List<Message> messages, List<ParseResult> parseResults, reactor.core.publisher.FluxSink<Object> sink) {
        try {
            // Process each tool call
            for (ParseResult result : parseResults) {
                if ("TOOL_CALL".equals(result.getType())) {
                    // Parse tool call data from JSON
                    ToolUseXmlParseRule.ToolUseData toolData;
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        toolData = objectMapper.readValue(result.getDisplayContent(), ToolUseXmlParseRule.ToolUseData.class);
                    } catch (Exception e) {
                        log.error("Failed to parse tool call data: {}", result.getDisplayContent(), e);
                        continue;
                    }

                    // Execute the tool
                    ToolCallResult toolResult = toolRegistry.executeTool(toolData.getToolName(), toolData.getToolParams());

                    log.info("Tool execution result: {} - {}", toolData.getToolName(), toolResult.isSuccess() ? "SUCCESS" : "FAILED");

                    // Add formatted tool result back to conversation
                    String formattedResult = toolResult.getFormatToolResult();
                    if (formattedResult != null) {
                        messages.add(new org.springframework.ai.chat.messages.UserMessage(formattedResult));
                    }
                }
            }

            // Make another chat call with tool results
            String finalResponse = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            // Emit final response
            ChatBlockResponse response = ChatBlockResponse.builder()
                    .conversationId(conversationId)
                    .status("COMPLETED")
                    .data(finalResponse)
                    .build();
            sink.next(response);
            sink.complete();

        } catch (Exception e) {
            log.error("Error handling tool calls for conversation: {}", conversationId, e);
            sink.error(e);
        }
    }
}
