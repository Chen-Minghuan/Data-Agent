package edu.zsc.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zsc.ai.config.PluginConfig;
import edu.zsc.ai.model.dto.request.ConnectRequest;
import edu.zsc.ai.model.dto.request.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.ConnectionResponse;
import edu.zsc.ai.model.dto.response.ConnectionTestResponse;
import edu.zsc.ai.model.enums.ConnectionTestStatus;
import edu.zsc.ai.service.ConnectionService;
import edu.zsc.ai.service.DbConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ConnectionController
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@WebMvcTest(ConnectionController.class)
@Import(PluginConfig.class)
class ConnectionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private DbConnectionService dbConnectionService;

    private ConnectRequest testRequest;
    private ConnectionCreateRequest createRequest;
    private ConnectionResponse connectionResponse;
    
    @BeforeEach
    void setUp() {
        testRequest = ConnectRequest.builder()
                .dbType("MYSQL")
                .host("localhost")
                .port(3306)
                .database("testdb")
                .username("root")
                .password("password")
                .driverJarPath("/path/to/mysql-connector.jar")
                .timeout(30)
                .build();

        createRequest = ConnectionCreateRequest.builder()
                .name("Test Connection")
                .dbType("MYSQL")
                .host("localhost")
                .port(3306)
                .database("testdb")
                .username("root")
                .password("password")
                .driverJarPath("/path/to/mysql-connector.jar")
                .timeout(30)
                .properties(new HashMap<>())
                .build();

        Map<String, String> properties = new HashMap<>();
        properties.put("useSSL", "false");
        properties.put("serverTimezone", "UTC");

        connectionResponse = ConnectionResponse.builder()
                .id(1L)
                .name("Test Connection")
                .dbType("MYSQL")
                .host("localhost")
                .port(3306)
                .database("testdb")
                .username("root")
                .driverJarPath("/path/to/mysql-connector.jar")
                .timeout(30)
                .properties(properties)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testConnection_Success() throws Exception {
        // Arrange
        ConnectionTestResponse testResponse = ConnectionTestResponse.builder()
                .status(ConnectionTestStatus.SUCCEEDED)
                .dbmsInfo("MySQL (ver. 8.0.43)")
                .driverInfo("MySQL Connector/J (ver. mysql-connector-j-8.2.0, JDBC4.2)")
                .ping(14L)
                .build();
        
        when(connectionService.testConnection(any(ConnectRequest.class))).thenReturn(testResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/connections/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.dbmsInfo").value("MySQL (ver. 8.0.43)"))
                .andExpect(jsonPath("$.data.driverInfo").value("MySQL Connector/J (ver. mysql-connector-j-8.2.0, JDBC4.2)"))
                .andExpect(jsonPath("$.data.ping").value(14));
        
        verify(connectionService).testConnection(any(ConnectRequest.class));
    }
    
    @Test
    void testConnection_ValidationError_MissingDbType() throws Exception {
        // Arrange
        testRequest.setDbType(null);
        
        // Act & Assert
        mockMvc.perform(post("/api/connections/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());
        
        verify(connectionService, never()).testConnection(any(ConnectRequest.class));
    }
    
    @Test
    void testConnection_ValidationError_InvalidPort() throws Exception {
        // Arrange
        testRequest.setPort(70000);
        
        // Act & Assert
        mockMvc.perform(post("/api/connections/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());
        
        verify(connectionService, never()).testConnection(any(ConnectRequest.class));
    }
    
    @Test
    void closeConnection_Success() throws Exception {
        // Arrange
        String connectionId = "550e8400-e29b-41d4-a716-446655440000";
        doNothing().when(connectionService).closeConnection(connectionId);

        // Act & Assert
        mockMvc.perform(delete("/api/connections/active/{connectionId}", connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(connectionService).closeConnection(connectionId);
    }

    @Test
    void createConnection_Success() throws Exception {
        // Arrange
        when(dbConnectionService.createConnection(any(ConnectionCreateRequest.class)))
                .thenReturn(connectionResponse);

        // Act & Assert
        mockMvc.perform(post("/api/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Connection"))
                .andExpect(jsonPath("$.data.dbType").value("MYSQL"))
                .andExpect(jsonPath("$.data.host").value("localhost"))
                .andExpect(jsonPath("$.data.port").value(3306))
                .andExpect(jsonPath("$.data.database").value("testdb"))
                .andExpect(jsonPath("$.data.username").value("root"))
                .andExpect(jsonPath("$.data.timeout").value(30))
                .andExpect(jsonPath("$.data.properties.useSSL").value("false"))
                .andExpect(jsonPath("$.data.properties.serverTimezone").value("UTC"));

        verify(dbConnectionService).createConnection(any(ConnectionCreateRequest.class));
    }

    @Test
    void createConnection_ValidationError_MissingName() throws Exception {
        // Arrange
        createRequest.setName(null);

        // Act & Assert
        mockMvc.perform(post("/api/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(dbConnectionService, never()).createConnection(any(ConnectionCreateRequest.class));
    }

    @Test
    void getConnections_Success() throws Exception {
        // Arrange
        ConnectionResponse response2 = ConnectionResponse.builder()
                .id(2L)
                .name("Production MySQL")
                .dbType("MYSQL")
                .host("prod.mysql.server")
                .port(3306)
                .database("proddb")
                .username("admin")
                .driverJarPath("/path/to/mysql-connector.jar")
                .timeout(60)
                .properties(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<ConnectionResponse> connections = Arrays.asList(connectionResponse, response2);
        when(dbConnectionService.getAllConnections()).thenReturn(connections);

        // Act & Assert
        mockMvc.perform(get("/api/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Test Connection"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("Production MySQL"));

        verify(dbConnectionService).getAllConnections();
    }

    @Test
    void getConnectionById_Success() throws Exception {
        // Arrange
        Long connectionId = 1L;
        when(dbConnectionService.getConnectionById(connectionId)).thenReturn(connectionResponse);

        // Act & Assert
        mockMvc.perform(get("/api/connections/{id}", connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Connection"))
                .andExpect(jsonPath("$.data.dbType").value("MYSQL"))
                .andExpect(jsonPath("$.data.host").value("localhost"))
                .andExpect(jsonPath("$.data.port").value(3306));

        verify(dbConnectionService).getConnectionById(connectionId);
    }

    @Test
    void getConnectionById_NotFound() throws Exception {
        // Arrange
        Long connectionId = 999L;
        when(dbConnectionService.getConnectionById(connectionId))
                .thenThrow(new IllegalArgumentException("Connection not found with id: " + connectionId));

        // Act & Assert
        mockMvc.perform(get("/api/connections/{id}", connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Connection not found with id: " + connectionId))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(dbConnectionService).getConnectionById(connectionId);
    }

    @Test
    void updateConnection_Success() throws Exception {
        // Arrange
        Long connectionId = 1L;
        ConnectionResponse updatedResponse = ConnectionResponse.builder()
                .id(1L)
                .name("Updated Connection")
                .dbType("MYSQL")
                .host("updated.mysql.server")
                .port(3307)
                .database("updatedb")
                .username("root")
                .driverJarPath("/path/to/mysql-connector.jar")
                .timeout(45)
                .properties(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(dbConnectionService.updateConnection(eq(connectionId), any(ConnectionCreateRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/connections/{id}", connectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated Connection"));

        verify(dbConnectionService).updateConnection(eq(connectionId), any(ConnectionCreateRequest.class));
    }

    @Test
    void updateConnection_NotFound() throws Exception {
        // Arrange
        Long connectionId = 999L;
        when(dbConnectionService.updateConnection(eq(connectionId), any(ConnectionCreateRequest.class)))
                .thenThrow(new IllegalArgumentException("Connection not found with id: " + connectionId));

        // Act & Assert
        mockMvc.perform(put("/api/connections/{id}", connectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Connection not found with id: " + connectionId))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(dbConnectionService).updateConnection(eq(connectionId), any(ConnectionCreateRequest.class));
    }

    @Test
    void deleteConnection_Success() throws Exception {
        // Arrange
        Long connectionId = 1L;
        doNothing().when(dbConnectionService).deleteConnection(connectionId);

        // Act & Assert
        mockMvc.perform(delete("/api/connections/{id}", connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(dbConnectionService).deleteConnection(connectionId);
    }

    @Test
    void deleteConnection_NotFound() throws Exception {
        // Arrange
        Long connectionId = 999L;
        doThrow(new IllegalArgumentException("Connection not found with id: " + connectionId))
                .when(dbConnectionService).deleteConnection(connectionId);

        // Act & Assert
        mockMvc.perform(delete("/api/connections/{id}", connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Connection not found with id: " + connectionId))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(dbConnectionService).deleteConnection(connectionId);
    }
}

