package edu.zsc.ai.config.mcp;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP hybrid configuration with lifecycle management
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true", matchIfMissing = true)
public class McpConfig implements DisposableBean {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private final McpProperties properties;
    private List<McpClient> clients;

    @Bean
    public List<McpClient> mcpClients() {
        if (properties.getServers().isEmpty()) {
            return List.of();
        }

        clients = properties.getServers().entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(e -> createClient(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .toList();

        log.info("Initialized {} MCP client(s)", clients.size());
        return clients;
    }

    @Override
    public void destroy() {
        if (clients != null && !clients.isEmpty()) {
            log.info("Closing {} MCP client(s)", clients.size());
            clients.forEach(client -> {
                try {
                    if (client instanceof DefaultMcpClient defaultClient) {
                        defaultClient.close();
                        log.debug("Closed MCP client: {}", defaultClient.key());
                    }
                } catch (Exception e) {
                    log.error("Failed to close MCP client", e);
                }
            });
        }
    }

    private McpClient createClient(String name, McpProperties.ServerConfig config) {
        try {
            config.validate();
            List<String> command = buildCommand(config);

            McpTransport transport = StdioMcpTransport.builder()
                    .command(command)
                    .environment(config.getEnv())
                    .build();

            McpClient client = DefaultMcpClient.builder()
                    .transport(transport)
                    .key(name)
                    .build();

            if (config.isDebug()) {
                log.debug("Created MCP client '{}' with command: {}", name, command);
            }

            return client;

        } catch (Exception e) {
            log.error("Failed to create MCP client '{}'", name, e);
            return null;
        }
    }

    private List<String> buildCommand(McpProperties.ServerConfig config) {
        if (config.getPackageName() != null) {
            return List.of(getNpxCommand(), config.getPackageName());
        }

        if (config.getCommand() != null) {
            return buildFullCommand(config);
        }

        throw new IllegalArgumentException(
                "Either 'packageName' or 'command' must be specified");
    }

    private List<String> buildFullCommand(McpProperties.ServerConfig config) {
        String adaptedCommand = adaptCommand(config.getCommand());
        if (config.getArgs() != null && !config.getArgs().isEmpty()) {
            return List.of(
                            List.of(adaptedCommand),
                            config.getArgs()
                    ).stream()
                    .flatMap(List::stream)
                    .toList();
        }
        return List.of(adaptedCommand);
    }

    private String getNpxCommand() {
        return IS_WINDOWS ? "npx.cmd" : "npx";
    }

    private String adaptCommand(String cmd) {
        if (IS_WINDOWS && !cmd.endsWith(".cmd") && !cmd.endsWith(".exe")) {
            if (cmd.equals("npx") || cmd.equals("npm")) {
                return cmd + ".cmd";
            }
        }
        return cmd;
    }
}
