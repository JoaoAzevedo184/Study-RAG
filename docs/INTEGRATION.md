# Pontos de acoplamento para `retrieval` / `generation`

Este documento lista as **interfaces públicas** entregues pelos pacotes `config`,
`ingestion` e `api` (escopo deste agente) que os pacotes `retrieval` e
`generation` (outro agente) vão consumir para implementar UC-5 (`POST /query`).

Nada aqui altera o contrato `study-rag-openapi.yml` (imutável no MVP).

## 1. Beans de IA (`com.joaoazevedo.studyrag.config`)

| Bean | Origem | Uso em retrieval/generation |
| :--- | :--- | :--- |
| `VectorStore` (PgVectorStore) | autoconfig do starter pgvector | busca por similaridade (`similaritySearch`) |
| `EmbeddingModel` (Ollama `nomic-embed-text`, 768d) | autoconfig do starter ollama | embedding da pergunta (usado internamente pelo VectorStore) |
| `ChatClient` | `AiConfig#chatClient(...)` | geração da resposta fundamentada (RN-6) |

> O `ChatClient` é exposto **sem** advisors/prompt de fundamentação — a montagem
> do prompt (RN-6) e dos advisors de citação (RN-7) é responsabilidade de
> `generation`.

## 2. Chaves de metadados dos chunks — `ingestion.ChunkMetadata`

Cada chunk é gravado no `VectorStore` com estes metadados. **Use estas constantes**
ao montar filtros e citações (não hard-code as strings):

```java
ChunkMetadata.SOURCE_URI  // "source_uri"
ChunkMetadata.COLLECTION  // "collection"
ChunkMetadata.PAGE        // "page"  (Integer; ausente em Markdown)
ChunkMetadata.TITLE       // "title"
```

### Filtro por coleção (RN-8)

```java
var filter = new FilterExpressionBuilder()
        .eq(ChunkMetadata.COLLECTION, collection)
        .build();

List<Document> hits = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(question)
                .topK(topK)
                .filterExpression(filter)   // omitir quando collection == null (busca global)
                .build());
```

Cada `Document` retornado traz `getText()` (snippet), `getScore()` (similaridade)
e `getMetadata()` com as chaves acima — material direto para `SourceCitation`.

## 3. Validação de coleção para o 422 (RN-8) — `ingestion.CollectionService`

```java
boolean CollectionService.exists(String collection)
```

`POST /query` deve retornar **422 COLLECTION_NOT_FOUND** quando a coleção
informada não existe/está vazia. Lance a exceção de domínio compartilhada:

```java
if (request.collection() != null && !collectionService.exists(request.collection())) {
    throw new ApiException(ErrorCode.COLLECTION_NOT_FOUND,
            "Coleção não encontrada ou vazia: " + request.collection());
}
```

## 4. Enriquecimento de citações — `ingestion.SourceService`

```java
Optional<IngestedFile> SourceService.findBySourceUri(String sourceUri)
```

Dado o `source_uri` de um chunk, devolve o registro da fonte
(`title`, `collection`, `sourceType`, ...) para preencher `SourceCitation.title`
quando o metadado do chunk não bastar.

## 5. Erros (`com.joaoazevedo.studyrag.error`)

Pacote **neutro**, sem dependência de web — pode ser usado por qualquer camada:

- `ErrorCode` — enum com o status HTTP de cada código da Seção 7 do SDD-MVP,
  incluindo `COLLECTION_NOT_FOUND` (422).
- `ApiException(ErrorCode, message[, details])` — lançável de qualquer pacote.

O `api.ApiExceptionHandler` (`@RestControllerAdvice`) já traduz `ApiException`
para o schema `Error` do OpenAPI. **Não** é preciso criar outro handler em
`retrieval`/`generation`: basta lançar `ApiException`.

## 6. O que NÃO foi implementado (fora deste escopo)

- `POST /query`, pacotes `retrieval` e `generation` (do outro agente).
- Coluna `fts`/hybrid search e índice GIN do SDD §7 — são Nível 2. A tabela
  `vector_store` é gerenciada pelo `PgVectorStore` (`initialize-schema: true`),
  apenas com `embedding vector(768)` + índice HNSW cosseno.
