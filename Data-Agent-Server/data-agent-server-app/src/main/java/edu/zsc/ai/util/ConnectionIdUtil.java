package edu.zsc.ai.util;

/**
 * Unified String/Long type conversion for connectionId, conversationId, userId, etc.
 * InvocationParameters may contain Strings (from LLM tool params) while RequestContext/Service layer expects Long.
 */
public final class ConnectionIdUtil {

    private ConnectionIdUtil() {
    }

    /**
     * Safely converts an Object to Long. Supports null, Long, Number, String.
     */
    public static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            if (s.isEmpty()) return null;
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
