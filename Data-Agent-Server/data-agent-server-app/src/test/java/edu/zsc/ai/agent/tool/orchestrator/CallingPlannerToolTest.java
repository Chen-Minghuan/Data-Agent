package edu.zsc.ai.agent.tool.orchestrator;

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

class CallingPlannerToolTest {

    private PlannerSubAgent mockPlanner;
    private CallingPlannerTool tool;

    @BeforeEach
    void setUp() {
        ExplorerSubAgent mockExplorer = mock(ExplorerSubAgent.class);
        mockPlanner = mock(PlannerSubAgent.class);
        SubAgentProperties properties = new SubAgentProperties();
        SubAgentManager subAgentManager = new SubAgentManager(mockExplorer, mockPlanner, properties);
        tool = new CallingPlannerTool(subAgentManager);
    }

    private SchemaSummary buildTestSchema() {
        return SchemaSummary.builder()
                .objects(List.of(
                        ExploreObject.builder()
                                .catalog("analytics")
                                .schema("public")
                                .objectName("users")
                                .objectType("TABLE")
                                .objectDdl("CREATE TABLE users (id int8)")
                                .relevance("HIGH")
                                .build()
                ))
                .rawResponse("users object found")
                .build();
    }

    @Test
    void invokesPlanner() {
        SqlPlan plan = SqlPlan.builder()
                .rawResponse("SELECT 1")
                .tokenUsage(10)
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenReturn(plan);

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate revenue query", schemaJson, null);

        assertTrue(result.isSuccess());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void missingSchemaSummary_fails() {
        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query", null, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("schemaSummaryJson"));
    }

    @Test
    void invalidSchemaJson_fails() {
        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query", "invalid json{{{", null);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("parse"));
    }

    @Test
    void validSchemaJson_returnsRawResponse() {
        SqlPlan plan = SqlPlan.builder()
                .rawResponse("SELECT SUM(total) FROM orders")
                .tokenUsage(30)
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenReturn(plan);

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        AgentToolResult result = tool.callingPlannerSubAgent(
                "calculate total revenue", schemaJson, null);

        assertTrue(result.isSuccess());
        String resultText = (String) result.getResult();
        assertTrue(resultText.contains("SELECT SUM(total)"));
    }

    @Test
    void instructionWithOptimizationContext_passedThrough() {
        SqlPlan plan = SqlPlan.builder()
                .rawResponse("SELECT 1")
                .tokenUsage(10)
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenAnswer(invocation -> {
            PlannerRequest req = invocation.getArgument(0);
            assertTrue(req.getInstruction().contains("optimize"));
            return plan;
        });

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        tool.callingPlannerSubAgent(
                "optimize: SELECT * FROM old. DDL: CREATE TABLE old (id int). Index: idx_old_id",
                schemaJson, null);

        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void explorerEnvelope_isAcceptedAsSchemaInput() {
        SqlPlan plan = SqlPlan.builder()
                .rawResponse("SELECT * FROM users")
                .tokenUsage(10)
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenReturn(plan);

        ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                .taskResults(List.of(
                        ExplorerTaskResult.builder()
                                .taskId("explore-1")
                                .summaryText("Relevant objects: users (TABLE).")
                                .objects(buildTestSchema().getObjects())
                                .rawResponse(buildTestSchema().getRawResponse())
                                .build()
                ))
                .build();

        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query",
                JsonUtil.object2json(envelope),
                null);

        assertTrue(result.isSuccess());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }
}
