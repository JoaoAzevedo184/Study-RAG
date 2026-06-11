package com.joaoazevedo.studyrag.retrieval;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Ativa o binding de {@link RetrievalProperties} (pacote {@code retrieval}). */
@Configuration
@EnableConfigurationProperties(RetrievalProperties.class)
public class RetrievalConfig {
}
