package com.joaoazevedo.studyrag.generation;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.joaoazevedo.studyrag.ingestion.ChunkMetadata;

/**
 * Geração da resposta fundamentada (RN-6): monta o prompt com os chunks recuperados
 * como contexto e instrui o modelo a responder exclusivamente a partir deles. Usa o
 * {@code ChatClient} exposto por {@code config} (Ollama por padrão, Groq alternável
 * por configuração) — esta classe é agnóstica ao provedor.
 *
 * <p>O contexto é injetado como mensagens já materializadas (sem template), evitando
 * que chaves {@code { }} presentes no texto dos documentos quebrem a renderização.</p>
 */
@Service
public class AnswerGenerator {

    private final ChatClient chatClient;
    private final GenerationProperties properties;

    public AnswerGenerator(ChatClient chatClient, GenerationProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    /**
     * Gera a resposta a partir da pergunta e dos chunks de contexto. Assume contexto
     * não vazio — o caso "não sei" (RN-7) é tratado antes, no {@link QueryService}.
     */
    public GeneratedAnswer generate(String question, List<Document> context) {
        SystemMessage system = new SystemMessage(properties.getSystemPrompt());
        UserMessage user = new UserMessage(buildUserMessage(question, context));

        ChatResponse response = chatClient.prompt(new Prompt(List.of(system, user)))
                .call()
                .chatResponse();

        String answer = response.getResult().getOutput().getText();
        return new GeneratedAnswer(answer != null ? answer.strip() : "", tokensUsed(response));
    }

    private String buildUserMessage(String question, List<Document> context) {
        StringBuilder sb = new StringBuilder("CONTEXTO:\n");
        int i = 1;
        for (Document doc : context) {
            Object page = doc.getMetadata().get(ChunkMetadata.PAGE);
            sb.append("[").append(i++).append("] fonte: ")
                    .append(doc.getMetadata().get(ChunkMetadata.SOURCE_URI));
            if (page != null) {
                sb.append(" (página ").append(page).append(")");
            }
            sb.append("\n").append(doc.getText()).append("\n\n");
        }
        sb.append("PERGUNTA:\n").append(question);
        return sb.toString();
    }

    private int tokensUsed(ChatResponse response) {
        if (response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return 0;
        }
        Usage usage = response.getMetadata().getUsage();
        Integer total = usage.getTotalTokens();
        return total != null ? total : 0;
    }
}
