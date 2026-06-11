package com.joaoazevedo.studyrag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans de IA portáveis (Spring AI). O {@code EmbeddingModel} (Ollama
 * nomic-embed-text) e o {@code VectorStore} (PgVectorStore) são autoconfigurados
 * pelos respectivos starters a partir do {@code application.yml}; aqui expomos
 * apenas o {@link ChatClient}, que o pacote {@code generation} (outro agente)
 * consome para a etapa de geração de respostas.
 */
@Configuration
@EnableConfigurationProperties(IngestionProperties.class)
public class AiConfig {

    /**
     * {@link ChatClient} base, construído a partir do builder autoconfigurado.
     * A montagem do prompt de fundamentação (RN-6) e dos advisors de
     * citação é responsabilidade do pacote {@code generation}.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
