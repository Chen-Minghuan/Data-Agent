package edu.zsc.ai.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    private boolean enabled = true;

    private Embedding embedding = new Embedding();

    private Retrieval retrieval = new Retrieval();

    @Data
    public static class Embedding {

        private int dimension = 1024;
    }

    @Data
    public static class Retrieval {

        private int preloadTopK = 6;

        private int candidateTopK = 10;

        private double minScore = 0.72;
    }
}
