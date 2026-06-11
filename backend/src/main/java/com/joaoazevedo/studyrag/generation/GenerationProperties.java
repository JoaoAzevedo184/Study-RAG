package com.joaoazevedo.studyrag.generation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da etapa de geração (RN-6 fundamentação, RN-7 citação). O provedor
 * do LLM é definido na camada {@code config}/{@code application.yml} (Ollama
 * {@code llama3.2:3b-instruct} por padrão, com Groq alternável por configuração);
 * esta camada é agnóstica ao provedor — apenas consome o {@code ChatClient}.
 */
@ConfigurationProperties(prefix = "studyrag.generation")
public class GenerationProperties {

    /**
     * Instrução de sistema que ancora a geração no contexto (RN-6) e exige que o
     * modelo declare desconhecimento quando o contexto for insuficiente.
     */
    private String systemPrompt = """
            Você é um assistente que responde EXCLUSIVAMENTE com base no CONTEXTO fornecido.
            Regras:
            - Não use conhecimento próprio nem informações externas ao contexto.
            - Se o contexto não contiver a resposta, responda exatamente: "%s".
            - Responda em português, de forma objetiva, citando apenas o que está no contexto.
            """.formatted(DEFAULT_NO_ANSWER_MESSAGE);

    /** Mensagem de "não sei" devolvida quando não há contexto relevante (RN-7). */
    private String noAnswerMessage = DEFAULT_NO_ANSWER_MESSAGE;

    static final String DEFAULT_NO_ANSWER_MESSAGE =
            "Não encontrei essa informação nos documentos disponíveis.";

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getNoAnswerMessage() {
        return noAnswerMessage;
    }

    public void setNoAnswerMessage(String noAnswerMessage) {
        this.noAnswerMessage = noAnswerMessage;
    }
}
