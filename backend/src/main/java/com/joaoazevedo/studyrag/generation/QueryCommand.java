package com.joaoazevedo.studyrag.generation;

/**
 * Pedido de consulta ao acervo (UC-5), desacoplado dos DTOs web do pacote
 * {@code api}. O controller {@code POST /query} traduz o {@code QueryRequest} do
 * OpenAPI neste comando.
 *
 * @param question  pergunta do usuário (validação de tamanho fica no DTO).
 * @param collection coleção a restringir a busca (RN-8); {@code null} busca em todo o acervo.
 * @param topK      número de chunks a recuperar; {@code null} usa o default configurado.
 */
public record QueryCommand(String question, String collection, Integer topK) {
}
