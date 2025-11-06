package edu.zsc.ai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON Utility Class based on Jackson ObjectMapper
 * Provides convenient JSON conversion methods.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert Map<String, String> to JSON string
     *
     * @param map the map to convert, can be null or empty
     * @return JSON string representation, or "{}" if input is null/empty or
     *         conversion fails
     */
    public static String mapToJson(Map<String, String> map) {
        if (MapUtils.isEmpty(map)) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert map to JSON string", e);
            return "{}";
        }
    }

    /**
     * Convert JSON string to Map<String, String>
     *
     * @param json the JSON string to convert, can be null or empty
     * @return Map with string key-value pairs, or empty map if input is null/empty
     *         or parsing fails
     */
    public static Map<String, String> jsonToMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON string to map: {}", json, e);
            return new HashMap<>();
        }
    }
}