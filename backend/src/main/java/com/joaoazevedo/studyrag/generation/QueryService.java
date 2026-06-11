package com.joaoazevedo.studyrag.generation;

import com.joaoazevedo.studyrag.error.ApiException;

/**
 * Ponto de entrada do pipeline RAG de consulta (UC-5). É a interface que o
 * controller {@code POST /query} (pacote {@code api}, outro agente) consome: ele
 * traduz o {@code QueryRequest} do OpenAPI num {@link QueryCommand}, chama
 * {@link #query(QueryCommand)} e mapeia o {@link QueryResult} no {@code QueryResponse}.
 */
public interface QueryService {

    /**
     * Recupera os chunks relevantes (respeitando o escopo de coleção — RN-8), gera a
     * resposta fundamentada (RN-6) e monta o resultado com fontes citadas (RN-7) e
     * métricas. Sempre status 200 do ponto de vista do contrato, inclusive no caso
     * "não sei" (sources vazio).
     *
     * @throws ApiException com {@code COLLECTION_NOT_FOUND} (422) quando a coleção
     *                      informada não existe ou está vazia (RN-8).
     */
    QueryResult query(QueryCommand command);
}
