package edu.zsc.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.spring.AiService;
import edu.zsc.ai.model.request.ChatRequest;

@AiService
public interface ReActAgent {

    @SystemMessage("classpath:prompt/system.md")
    TokenStream chat(ChatRequest request);
}
