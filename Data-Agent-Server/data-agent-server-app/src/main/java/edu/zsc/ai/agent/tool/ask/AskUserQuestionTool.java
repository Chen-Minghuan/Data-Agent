package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.UserQuestion;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for asking the user structured clarification questions.
 * Available in both Agent and Plan modes.
 */
@AgentTool
@Slf4j
public class AskUserQuestionTool {

    @Tool(
            value = {
                    "[GOAL] Ask the user structured clarification/selection questions to resolve ambiguity.",
                    "[WHEN] Call when intent is ambiguous, multiple candidates exist, or critical constraints are missing.",
                    "[WHEN_NOT] Do not use for write operation confirmation — use askUserConfirm. Do not ask questions that tools can answer (e.g., table existence, schema info).",
                    "[INPUT] Each question should have 2-3 options (max 3). Prefer concrete options over open-ended prompts."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        int count = questions == null ? 0 : questions.size();
        log.info("[Tool] askUserQuestion, {} question(s)", count);
        return count + " question(s) presented to user.";
    }
}
