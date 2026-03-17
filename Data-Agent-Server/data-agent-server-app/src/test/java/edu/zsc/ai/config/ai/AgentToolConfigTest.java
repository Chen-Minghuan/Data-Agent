package edu.zsc.ai.config.ai;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.ask.AskUserQuestionTool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.plan.EnterPlanModeTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.GetEnvironmentOverviewTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentToolConfigTest {

    private AgentToolConfig config;
    private List<Object> allTools;

    // Mocked tool instances — mock() creates real subclass instances so getClass() matches
    private GetEnvironmentOverviewTool getEnvironmentOverviewTool;
    private SearchObjectsTool searchObjectsTool;
    private GetObjectDetailTool getObjectDetailTool;
    private ExecuteSqlTool executeSqlTool;
    private AskUserConfirmTool askUserConfirmTool;
    private AskUserQuestionTool askUserQuestionTool;
    private TodoTool todoTool;
    private EnterPlanModeTool enterPlanModeTool;
    private ExitPlanModeTool exitPlanModeTool;
    private ActivateSkillTool activateSkillTool;
    private ChartTool chartTool;

    @BeforeEach
    void setUp() {
        config = new AgentToolConfig();

        getEnvironmentOverviewTool = mock(GetEnvironmentOverviewTool.class);
        searchObjectsTool = mock(SearchObjectsTool.class);
        getObjectDetailTool = mock(GetObjectDetailTool.class);
        executeSqlTool = mock(ExecuteSqlTool.class);
        askUserConfirmTool = mock(AskUserConfirmTool.class);
        askUserQuestionTool = mock(AskUserQuestionTool.class);
        todoTool = mock(TodoTool.class);
        enterPlanModeTool = mock(EnterPlanModeTool.class);
        exitPlanModeTool = mock(ExitPlanModeTool.class);
        activateSkillTool = mock(ActivateSkillTool.class);
        chartTool = mock(ChartTool.class);

        allTools = List.of(
                getEnvironmentOverviewTool,
                searchObjectsTool,
                getObjectDetailTool,
                executeSqlTool,
                askUserConfirmTool,
                askUserQuestionTool,
                todoTool,
                enterPlanModeTool,
                exitPlanModeTool,
                activateSkillTool,
                chartTool
        );
    }

    // ==================== 原有行为保持不变 ====================

    @Nested
    class LegacyModeFiltering {

        @Test
        void agentMode_disablesExitPlanModeTool() {
            List<Object> tools = config.filterTools(allTools, AgentModeEnum.AGENT);

            assertFalse(tools.contains(exitPlanModeTool),
                    "AGENT mode should disable ExitPlanModeTool");
            assertTrue(tools.contains(executeSqlTool),
                    "AGENT mode should keep ExecuteSqlTool");
        }

        @Test
        void planMode_disablesExecutionTools() {
            List<Object> tools = config.filterTools(allTools, AgentModeEnum.PLAN);

            assertFalse(tools.contains(executeSqlTool),
                    "PLAN mode should disable ExecuteSqlTool");
            assertFalse(tools.contains(chartTool),
                    "PLAN mode should disable ChartTool");
            assertFalse(tools.contains(askUserConfirmTool),
                    "PLAN mode should disable AskUserConfirmTool");
            assertTrue(tools.contains(exitPlanModeTool),
                    "PLAN mode should keep ExitPlanModeTool");
        }
    }

    // ==================== 新增：按 AgentType 过滤 ====================

    @Nested
    class AgentTypeFiltering {

        @Test
        void mainAgent_hasGetEnvironmentOverviewButNotSearchOrDetail() {
            List<Object> tools = config.filterToolsByAgentType(allTools, AgentTypeEnum.MAIN);

            assertTrue(tools.contains(executeSqlTool), "MAIN should have ExecuteSqlTool");
            assertTrue(tools.contains(askUserConfirmTool), "MAIN should have AskUserConfirmTool");
            assertTrue(tools.contains(askUserQuestionTool), "MAIN should have AskUserQuestionTool");
            assertTrue(tools.contains(todoTool), "MAIN should have TodoTool");
            assertTrue(tools.contains(chartTool), "MAIN should have ChartTool");
            assertTrue(tools.contains(activateSkillTool), "MAIN should have ActivateSkillTool");
            assertTrue(tools.contains(enterPlanModeTool), "MAIN should have EnterPlanModeTool");

            assertTrue(tools.contains(getEnvironmentOverviewTool), "MAIN should have GetEnvironmentOverviewTool for connection list");
            assertFalse(tools.contains(searchObjectsTool), "MAIN should NOT have SearchObjectsTool — via callingExplorerSubAgent");
            assertFalse(tools.contains(getObjectDetailTool), "MAIN should NOT have GetObjectDetailTool — via callingExplorerSubAgent");
        }

        @Test
        void explorer_hasDiscoveryTools() {
            List<Object> tools = config.filterToolsByAgentType(allTools, AgentTypeEnum.EXPLORER);

            assertEquals(2, tools.size(), "Explorer should have 2 scoped discovery tools");
            assertFalse(tools.contains(getEnvironmentOverviewTool), "Explorer should NOT have GetEnvironmentOverviewTool");
            assertTrue(tools.contains(searchObjectsTool), "Explorer should have SearchObjectsTool");
            assertTrue(tools.contains(getObjectDetailTool), "Explorer should have GetObjectDetailTool");
        }

        @Test
        void planner_hasTodoAndSkill() {
            List<Object> tools = config.filterToolsByAgentType(allTools, AgentTypeEnum.PLANNER);

            assertEquals(2, tools.size(), "Planner should have exactly 2 tools");
            assertTrue(tools.contains(todoTool), "Planner should have TodoTool");
            assertTrue(tools.contains(activateSkillTool), "Planner should have ActivateSkillTool");
        }

        @Test
        void planner_excludesExecutionAndDiscoveryTools() {
            List<Object> tools = config.filterToolsByAgentType(allTools, AgentTypeEnum.PLANNER);

            assertFalse(tools.contains(executeSqlTool), "Planner should NOT have ExecuteSqlTool");
            assertFalse(tools.contains(getEnvironmentOverviewTool), "Planner should NOT have GetEnvironmentOverviewTool");
            assertFalse(tools.contains(searchObjectsTool), "Planner should NOT have SearchObjectsTool");
            assertFalse(tools.contains(getObjectDetailTool), "Planner should NOT have GetObjectDetailTool");
            assertFalse(tools.contains(askUserConfirmTool), "Planner should NOT have AskUserConfirmTool");
            assertFalse(tools.contains(chartTool), "Planner should NOT have ChartTool");
        }

        @Test
        void mainAgent_planMode_combinesBothFilters() {
            // First filter by AgentType, then by AgentMode
            List<Object> mainTools = config.filterToolsByAgentType(allTools, AgentTypeEnum.MAIN);
            List<Object> planTools = config.filterTools(mainTools, AgentModeEnum.PLAN);

            // PLAN mode should further disable ExecuteSql, Chart, AskUserConfirm
            assertFalse(planTools.contains(executeSqlTool), "MAIN+PLAN should disable ExecuteSqlTool");
            assertFalse(planTools.contains(chartTool), "MAIN+PLAN should disable ChartTool");
            assertFalse(planTools.contains(askUserConfirmTool), "MAIN+PLAN should disable AskUserConfirmTool");

            // TodoTool stays
            assertTrue(planTools.contains(todoTool), "MAIN+PLAN should keep TodoTool");
        }
    }

    @Test
    void buildToolExecutors_supportsCglibProxyAndInvokesProxyMethod() {
        AtomicInteger adviceCounter = new AtomicInteger();
        EchoTool target = new EchoTool();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            adviceCounter.incrementAndGet();
            return invocation.proceed();
        });
        Object proxy = proxyFactory.getProxy();

        Map<ToolSpecification, ToolExecutor> executors = config.buildToolExecutors(List.of(proxy));

        assertEquals(1, executors.size());

        Map.Entry<ToolSpecification, ToolExecutor> registration = executors.entrySet().iterator().next();
        assertEquals("echo", registration.getKey().name());

        String result = registration.getValue().execute(
                ToolExecutionRequest.builder()
                        .name("echo")
                        .arguments("{\"value\":\"hello\"}")
                        .build(),
                null
        );

        assertEquals("echo:hello", result);
        assertEquals(1, adviceCounter.get(), "tool execution should still go through proxy advice");
    }

    @AgentTool
    static class EchoTool {

        @Tool
        public String echo(@P("Echo value") String value) {
            return "echo:" + value;
        }
    }
}
