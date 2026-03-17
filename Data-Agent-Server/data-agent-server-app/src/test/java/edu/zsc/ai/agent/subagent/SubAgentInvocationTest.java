package edu.zsc.ai.agent.subagent;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.explorer.ExplorerAgentService;
import edu.zsc.ai.agent.subagent.planner.PlannerAgentService;
import edu.zsc.ai.common.constant.RequestContextConstant;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SubAgent invocation parameter passing and @Agent interface signatures.
 */
class SubAgentInvocationTest {

    @Test
    void explorerAgentService_hasCorrectMethodSignature() throws Exception {
        var method = ExplorerAgentService.class.getMethod("explore", String.class);
        assertNotNull(method);
        assertEquals(1, method.getParameterCount());
        assertEquals(TokenStream.class, method.getReturnType());
    }

    @Test
    void plannerAgentService_hasCorrectMethodSignature() throws Exception {
        var method = PlannerAgentService.class.getMethod("plan", String.class);
        assertNotNull(method);
        assertEquals(1, method.getParameterCount());
        assertEquals(TokenStream.class, method.getReturnType());
    }

    @Test
    void invocationParameters_canBeBuiltFromRequestContext() {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put(RequestContextConstant.USER_ID, "42");
        contextMap.put(RequestContextConstant.CONVERSATION_ID, "100");
        contextMap.put(RequestContextConstant.CONNECTION_ID, "5");

        InvocationParameters params = InvocationParameters.from(contextMap);
        assertEquals("42", params.get(RequestContextConstant.USER_ID));
        assertEquals("100", params.get(RequestContextConstant.CONVERSATION_ID));
        assertEquals("5", params.get(RequestContextConstant.CONNECTION_ID));
    }

    @Test
    void invocationParameters_preservesDatabaseContext() {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put(RequestContextConstant.CONNECTION_ID, "5");
        contextMap.put(RequestContextConstant.DATABASE_NAME, "mydb");
        contextMap.put(RequestContextConstant.SCHEMA_NAME, "public");

        InvocationParameters params = InvocationParameters.from(contextMap);
        assertEquals("mydb", params.get(RequestContextConstant.DATABASE_NAME));
        assertEquals("public", params.get(RequestContextConstant.SCHEMA_NAME));
    }
}
