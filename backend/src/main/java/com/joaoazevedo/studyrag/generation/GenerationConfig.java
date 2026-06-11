package com.joaoazevedo.studyrag.generation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Ativa o binding de {@link GenerationProperties} (pacote {@code generation}). */
@Configuration
@EnableConfigurationProperties(GenerationProperties.class)
public class GenerationConfig {
}
