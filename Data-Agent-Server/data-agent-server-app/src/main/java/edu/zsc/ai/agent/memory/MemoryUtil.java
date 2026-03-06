package edu.zsc.ai.agent.memory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

/**
 * Utility for building and stripping memory/candidate context wrappers in user messages.
 */
public final class MemoryUtil {

    public static final String TAG_MEMORY_CONTEXT_OPEN = "<memory_context>";
    public static final String TAG_MEMORY_CONTEXT_CLOSE = "</memory_context>";
    public static final String TAG_CANDIDATE_CONTEXT_OPEN = "<candidate_context>";
    public static final String TAG_CANDIDATE_CONTEXT_CLOSE = "</candidate_context>";
    public static final String TAG_USER_QUERY_OPEN = "<user_query>";
    public static final String TAG_USER_QUERY_CLOSE = "</user_query>";

    private static final String MEMORY_PREFIX = "- [M";
    private static final String CANDIDATE_PREFIX = "- [C";
    private static final String BRACKET_OPEN = "[";
    private static final String BRACKET_CLOSE = "]";
    private static final String SCORE_FORMAT = "%.4f";
    private static final String TRUNCATE_SUFFIX = "...";

    private static final int MEMORY_CONTENT_MAX_LENGTH = 240;
    private static final int CANDIDATE_CONTENT_MAX_LENGTH = 220;

    private static final Pattern USER_QUERY_PATTERN =
            Pattern.compile("(?s)" + TAG_USER_QUERY_OPEN + "\\s*(.*?)\\s*" + TAG_USER_QUERY_CLOSE);

    private MemoryUtil() {
    }

    /**
     * Formats memory search results into {@code <memory_context>} XML block.
     */
    public static String formatMemoryContext(List<MemorySearchResult> memories) {
        if (CollectionUtils.isEmpty(memories)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(TAG_MEMORY_CONTEXT_OPEN).append('\n');
        for (MemorySearchResult memory : memories) {
            builder.append(MEMORY_PREFIX).append(memory.getId()).append(BRACKET_CLOSE)
                    .append(BRACKET_OPEN).append(memory.getMemoryType()).append(BRACKET_CLOSE)
                    .append(BRACKET_OPEN).append(String.format(SCORE_FORMAT, memory.getScore())).append(BRACKET_CLOSE)
                    .append(' ')
                    .append(truncate(memory.getContent(), MEMORY_CONTENT_MAX_LENGTH))
                    .append('\n');
        }
        builder.append(TAG_MEMORY_CONTEXT_CLOSE).append("\n\n");
        return builder.toString();
    }

    /**
     * Formats memory candidates into {@code <candidate_context>} XML block.
     */
    public static String formatCandidateContext(List<AiMemoryCandidate> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(TAG_CANDIDATE_CONTEXT_OPEN).append('\n');
        for (AiMemoryCandidate candidate : candidates) {
            builder.append(CANDIDATE_PREFIX).append(candidate.getId()).append(BRACKET_CLOSE)
                    .append(BRACKET_OPEN).append(candidate.getCandidateType()).append(BRACKET_CLOSE)
                    .append(' ')
                    .append(truncate(candidate.getCandidateContent(), CANDIDATE_CONTENT_MAX_LENGTH))
                    .append('\n');
        }
        builder.append(TAG_CANDIDATE_CONTEXT_CLOSE).append("\n\n");
        return builder.toString();
    }

    /**
     * Builds enriched user message with memory and candidate context.
     * Returns the original message unchanged if both lists are empty.
     */
    public static String buildEnrichedMessage(String userMessage,
                                               List<MemorySearchResult> memories,
                                               List<AiMemoryCandidate> candidates) {
        String memoryBlock = formatMemoryContext(memories);
        String candidateBlock = formatCandidateContext(candidates);
        if (StringUtils.isEmpty(memoryBlock) && StringUtils.isEmpty(candidateBlock)) {
            return userMessage;
        }
        return memoryBlock + candidateBlock
                + TAG_USER_QUERY_OPEN + "\n" + userMessage + "\n" + TAG_USER_QUERY_CLOSE;
    }

    /**
     * Normalizes a ChatMessage by stripping injected memory/candidate wrappers from UserMessage.
     * Non-UserMessage types are returned unchanged.
     */
    public static ChatMessage normalizeUserMessage(ChatMessage message) {
        if (!(message instanceof UserMessage userMessage)) {
            return message;
        }
        if (CollectionUtils.isEmpty(userMessage.contents())
                || !userMessage.contents().stream().allMatch(c -> c instanceof TextContent)) {
            return message;
        }
        String content = userMessage.contents().stream()
                .filter(c -> c instanceof TextContent)
                .map(c -> ((TextContent) c).text())
                .collect(Collectors.joining("\n"));
        if (StringUtils.isBlank(content)) {
            return message;
        }
        String normalized = stripInjectedWrapper(content);
        if (normalized.equals(content)) {
            return message;
        }
        return UserMessage.from(normalized);
    }

    /**
     * Strips injected wrappers, returning just the original user query text.
     * Returns the original content unchanged if no wrapper is detected.
     */
    public static String stripInjectedWrapper(String content) {
        String trimmed = StringUtils.trimToEmpty(content);
        if (!looksLikeInjectedWrapper(trimmed)) {
            return content;
        }
        Matcher matcher = USER_QUERY_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return content;
        }
        return StringUtils.trimToEmpty(matcher.group(1));
    }

    private static boolean looksLikeInjectedWrapper(String trimmed) {
        if (!trimmed.contains(TAG_USER_QUERY_OPEN) || !trimmed.contains(TAG_USER_QUERY_CLOSE)) {
            return false;
        }
        return trimmed.startsWith(TAG_MEMORY_CONTEXT_OPEN)
                || trimmed.startsWith(TAG_CANDIDATE_CONTEXT_OPEN)
                || trimmed.startsWith(TAG_USER_QUERY_OPEN);
    }

    private static String truncate(String value, int max) {
        String normalized = StringUtils.defaultString(value).replaceAll("\\s+", " ").trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + TRUNCATE_SUFFIX;
    }
}
