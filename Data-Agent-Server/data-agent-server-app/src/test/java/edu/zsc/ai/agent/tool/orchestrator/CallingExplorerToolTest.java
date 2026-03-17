package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.*;
import edu.zsc.ai.agent.subagent.explorer.ExplorerSubAgent;
import edu.zsc.ai.agent.subagent.planner.PlannerSubAgent;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CallingExplorerToolTest {

    private ExplorerSubAgent mockExplorer;
    private CallingExplorerTool tool;

    @BeforeEach
    void setUp() {
        mockExplorer = mock(ExplorerSubAgent.class);
        PlannerSubAgent mockPlanner = mock(PlannerSubAgent.class);
        SubAgentProperties properties = new SubAgentProperties();
        SubAgentManager subAgentManager = new SubAgentManager(mockExplorer, mockPlanner, properties);
        tool = new CallingExplorerTool(subAgentManager);
    }

    private SchemaSummary buildTestSchema(String tableName) {
        return SchemaSummary.builder()
                .objects(List.of(
                        ExploreObject.builder()
                                .catalog("analytics")
                                .schema("public")
                                .objectName(tableName)
                                .objectType("TABLE")
                                .objectDdl("CREATE TABLE " + tableName + " (id int8)")
                                .relevance("HIGH")
                                .build()
                ))
                .rawResponse("explored " + tableName)
                .build();
    }

    @Test
    void singleTask_invokesExplorer() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenReturn(buildTestSchema("users"));

        String tasksJson = "[{\"connectionId\":1,\"instruction\":\"explore all tables\"}]";
        AgentToolResult result = tool.callingExplorerSubAgent(tasksJson, null, null);

        assertTrue(result.isSuccess());
        ExplorerResultEnvelope envelope = JsonUtil.json2Object((String) result.getResult(), ExplorerResultEnvelope.class);
        assertEquals(1, envelope.getTaskResults().size());
        assertEquals("users", envelope.getTaskResults().get(0).getObjects().get(0).getObjectName());
        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }

    @Test
    void nullTasks_fails() {
        AgentToolResult result = tool.callingExplorerSubAgent(null, null, null);
        assertFalse(result.isSuccess());
    }

    @Test
    void emptyArray_fails() {
        AgentToolResult result = tool.callingExplorerSubAgent("[]", null, null);
        assertFalse(result.isSuccess());
    }

    @Test
    void multipleTasks_invokesConcurrently() {
        when(mockExplorer.invoke(any(SubAgentRequest.class)))
                .thenReturn(buildTestSchema("users"))
                .thenReturn(buildTestSchema("orders"));

        String tasksJson = "[{\"connectionId\":1,\"instruction\":\"explore users\"},{\"connectionId\":2,\"instruction\":\"explore orders\"}]";
        AgentToolResult result = tool.callingExplorerSubAgent(tasksJson, null, null);

        assertTrue(result.isSuccess());
        verify(mockExplorer, times(2)).invoke(any(SubAgentRequest.class));
    }

    @Test
    void multipleTasks_returnsTaskResultsEnvelope() {
        when(mockExplorer.invoke(any(SubAgentRequest.class)))
                .thenReturn(buildTestSchema("users"))
                .thenReturn(buildTestSchema("orders"));

        String tasksJson = "[{\"connectionId\":1,\"instruction\":\"explore users\"},{\"connectionId\":2,\"instruction\":\"explore orders\"}]";
        AgentToolResult result = tool.callingExplorerSubAgent(tasksJson, null, null);

        assertTrue(result.isSuccess());
        ExplorerResultEnvelope envelope = JsonUtil.json2Object((String) result.getResult(), ExplorerResultEnvelope.class);
        assertEquals(2, envelope.getTaskResults().size());
        List<String> objectNames = envelope.getTaskResults().stream()
                .map(taskResult -> taskResult.getObjects().get(0).getObjectName())
                .sorted()
                .toList();
        assertEquals(List.of("orders", "users"), objectNames);
    }

    @Test
    void context_passedToRequest() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenAnswer(invocation -> {
            SubAgentRequest req = invocation.getArgument(0);
            assertEquals("previous error", req.context());
            return buildTestSchema("users");
        });

        String tasksJson = "[{\"connectionId\":1,\"instruction\":\"retry\",\"context\":\"previous error\"}]";
        tool.callingExplorerSubAgent(tasksJson, null, null);

        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }
}
