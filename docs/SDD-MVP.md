---
documento: Especificação Spec-Driven Development (SDD)
projeto: study-rag
nível: 1 (MVP)
versão: 1.0
autor: João Victor Azevedo
data: 2026-06-10
contrato: study-rag-openapi.yaml (fonte de verdade dos endpoints)
status: pronta para implementação
---

# study-rag — Especificação Spec-Driven (Nível 1 / MVP)

Esta especificação é a **fonte de verdade** do MVP. Ela descreve o comportamento esperado antes do código: regras de negócio explícitas, validações, cenários de erro e critérios de aceite testáveis. O contrato `study-rag-openapi.yaml` acompanha este documento e define formalmente os endpoints; backend, frontend e testes trabalham com ambos como referência única.

O princípio que guia o nível de detalhe: escrever o suficiente para que a implementação seja **inevitável e objetiva**, sem decisões ambíguas durante a codificação.

## 1. Escopo do MVP

O Nível 1 entrega um RAG funcional ponta a ponta na forma mais simples que agrega valor real. Estão dentro do escopo: ingestão de arquivos PDF e Markdown, deduplicação por hash, organização em coleções, chunking recursivo, geração de embeddings via Ollama, persistência em pgvector, recuperação por similaridade pura e geração de resposta com citação obrigatória de fontes.

Estão **fora** do escopo do MVP (níveis posteriores): hybrid search, reranking, contextual retrieval, web scraping de sites, ingestão de vídeo, streaming SSE, avaliação automatizada e agentic RAG. A ausência dessas funcionalidades não pode quebrar nenhum critério de aceite abaixo.

## 2. Atores e contexto

O sistema é **single-user**, operado pelo próprio autor no homelab. Não há autenticação no MVP. Os atores lógicos são o **Usuário** (que ingere documentos e faz perguntas pela interface React) e o **Sistema** (a API que executa o pipeline RAG). Modelos de IA rodam localmente via Ollama, sem dependência de nuvem no caminho crítico do MVP.

## 3. Entidades de domínio

A entidade **Fonte (Source)** representa um documento ingerido, identificado por um `fileId` único e por um `sourceUri` único (não pode haver duas fontes com o mesmo caminho). Cada fonte pertence a exatamente uma **Coleção**, tem um tipo (`pdf` ou `markdown`), um hash SHA-256 do conteúdo extraído, e uma contagem de chunks gerados.

A entidade **Coleção (Collection)** é um agrupamento nomeado por tema. Não é uma tabela própria no MVP: é derivada do campo `collection` das fontes e dos metadados dos chunks. Toda fonte sem coleção explícita pertence à coleção `default`.

A entidade **Chunk** é um fragmento de texto com seu embedding e metadados (`source_uri`, `collection`, `page` quando aplicável). É gerenciada pelo `PgVectorStore` do Spring AI e não é exposta diretamente na API — apenas aflora como `SourceCitation` nas respostas.

## 4. Regras de negócio

As regras abaixo são normativas. Cada uma tem um identificador (RN-x) referenciado pelos critérios de aceite.

**RN-1 — Tipos suportados.** No MVP, apenas `sourceType` igual a `pdf` ou `markdown` é aceito na ingestão. Qualquer outro valor é rejeitado com erro de validação, e qualquer arquivo cujo conteúdo não possa ser extraído como texto é rejeitado como não suportado.

**RN-2 — Unicidade de fonte.** O `sourceUri` é único no sistema. Ingerir um `sourceUri` já existente não cria uma segunda fonte; aciona a regra de deduplicação (RN-3).

**RN-3 — Deduplicação por hash.** Antes de processar, o sistema calcula o SHA-256 do conteúdo de texto extraído. Se já existe uma fonte com o mesmo `sourceUri` **e** o mesmo hash, a ingestão é pulada e retorna `SKIPPED_UNCHANGED` com `chunkCount` zero, sem gerar novos embeddings. Se o `sourceUri` existe mas o hash mudou, os chunks antigos daquela fonte são removidos e o documento é reindexado (atualização incremental).

**RN-4 — Nomenclatura de coleção.** O nome da coleção é um slug em minúsculas, aceitando letras, números e hífens (`^[a-z0-9-]+$`), com no máximo 80 caracteres. Ausência de coleção atribui `default`. Nomes inválidos são rejeitados por validação.

**RN-5 — Chunking determinístico.** O chunking usa estratégia recursiva com tamanho-alvo de 1000 caracteres e overlap de 200, parâmetros fixos no MVP. A mesma entrada produz sempre os mesmos chunks, garantindo reprodutibilidade.

**RN-6 — Fundamentação obrigatória.** A geração responde **exclusivamente** com base nos chunks recuperados. O prompt instrui o modelo a não usar conhecimento próprio e a declarar que não sabe quando o contexto for insuficiente.

**RN-7 — Citação obrigatória.** Toda resposta que afirma fatos deve vir acompanhada das fontes que a embasaram, no campo `sources`. Quando o sistema responde "não sei" por falta de contexto relevante, `sources` é uma lista vazia.

**RN-8 — Escopo por coleção.** Quando o pedido de query informa uma `collection`, a recuperação considera apenas chunks daquela coleção. Sem coleção informada, busca em todo o acervo.

**RN-9 — Remoção em cascata.** Remover uma fonte apaga o registro da fonte e todos os seus chunks no vector store, de forma atômica do ponto de vista do usuário.

## 5. Especificação funcional por caso de uso

### UC-1 — Ingerir documento

O Usuário fornece um `sourceType`, um `sourceUri` apontando para um arquivo no diretório de uploads, opcionalmente uma `collection` e `metadata`. O Sistema valida os campos (RN-1, RN-4), localiza o arquivo, extrai o texto, calcula o hash e aplica a deduplicação (RN-3). Quando há processamento, fragmenta o texto (RN-5), gera embeddings via Ollama, persiste os chunks com seus metadados no pgvector e registra a fonte. Retorna `202` com o `fileId`, o `status` e a contagem de chunks.

Cenários de erro: arquivo inexistente retorna `404`; tipo não suportado retorna `415`; campos inválidos retornam `400`.

### UC-2 — Listar fontes

O Usuário solicita a lista de fontes, opcionalmente filtrada por `collection`. O Sistema retorna `200` com os metadados de cada fonte (tipo, título, coleção, contagem de chunks, status, data). Lista vazia é uma resposta válida.

### UC-3 — Remover fonte

O Usuário informa um `fileId`. O Sistema remove a fonte e seus chunks (RN-9) e retorna `204`. Se o `fileId` não existe, retorna `404`.

### UC-4 — Listar coleções

O Usuário solicita as coleções existentes. O Sistema agrega as fontes por coleção e retorna `200` com nome, número de fontes e número de chunks de cada uma.

### UC-5 — Perguntar ao acervo

O Usuário envia uma `question`, opcionalmente uma `collection` e um `topK`. O Sistema valida a pergunta (tamanho mínimo 3, máximo 2000), recupera os `topK` chunks mais similares respeitando o escopo de coleção (RN-8), monta o prompt com fundamentação obrigatória (RN-6) e gera a resposta. Retorna `200` com a resposta, as fontes citadas (RN-7) e as métricas de execução.

Cenários de borda: quando nenhum chunk relevante é encontrado, o Sistema responde com a mensagem de "não sei" e `sources` vazio (não é erro). Quando a `collection` informada não existe ou está vazia, retorna `422`.

## 6. Critérios de aceite (Gherkin)

Os cenários abaixo são executáveis como testes de aceitação. Cada um referencia as regras de negócio que valida.

```gherkin
Funcionalidade: Ingestão de documentos

  Cenário: Ingerir um PDF novo com sucesso
    Dado que o arquivo "/uploads/guia.pdf" existe no diretório de uploads
    E não existe nenhuma fonte com esse sourceUri
    Quando eu envio POST /ingest com sourceType "pdf" e collection "docs"
    Então a resposta tem status 202
    E o status do corpo é "INGESTED"
    E chunkCount é maior que zero

  Cenário: Ingerir o mesmo conteúdo é idempotente (RN-3)
    Dado que "/uploads/guia.pdf" já foi ingerido com sucesso
    E o conteúdo do arquivo não mudou
    Quando eu envio POST /ingest para o mesmo sourceUri
    Então a resposta tem status 202
    E o status do corpo é "SKIPPED_UNCHANGED"
    E chunkCount é igual a zero
    E nenhum novo chunk é criado no vector store

  Cenário: Reindexar quando o conteúdo muda (RN-3)
    Dado que "/uploads/guia.pdf" já foi ingerido com 40 chunks
    E o conteúdo do arquivo foi alterado
    Quando eu envio POST /ingest para o mesmo sourceUri
    Então o status do corpo é "INGESTED"
    E os 40 chunks antigos não existem mais no vector store

  Cenário: Rejeitar tipo não suportado no MVP (RN-1)
    Quando eu envio POST /ingest com sourceType "docx"
    Então a resposta tem status 400

  Cenário: Arquivo inexistente
    Quando eu envio POST /ingest com sourceUri "/uploads/nao-existe.pdf"
    Então a resposta tem status 404

  Cenário: Nome de coleção inválido (RN-4)
    Quando eu envio POST /ingest com collection "Docs Spring!"
    Então a resposta tem status 400
```

```gherkin
Funcionalidade: Consulta ao acervo

  Cenário: Pergunta com resposta fundamentada e fontes (RN-6, RN-7)
    Dado que a coleção "docs" contém documentos sobre chunking
    Quando eu envio POST /query com question "O que é chunking recursivo?" e collection "docs"
    Então a resposta tem status 200
    E o campo answer não está vazio
    E sources contém ao menos um item
    E cada item de sources tem sourceUri, snippet e score

  Cenário: Pergunta sem contexto relevante responde "não sei" (RN-7)
    Dado que a coleção "docs" não contém nada sobre culinária
    Quando eu envio POST /query com question "Como fazer um risoto?" e collection "docs"
    Então a resposta tem status 200
    E sources é uma lista vazia
    E answer indica que a informação não foi encontrada

  Cenário: Escopo restrito à coleção (RN-8)
    Dado que existe a coleção "spring" e a coleção "django"
    Quando eu envio POST /query com collection "spring"
    Então nenhuma fonte citada pertence à coleção "django"

  Cenário: Coleção inexistente (RN-8)
    Quando eu envio POST /query com collection "inexistente"
    Então a resposta tem status 422

  Cenário: Pergunta curta demais é rejeitada
    Quando eu envio POST /query com question "oi"
    Então a resposta tem status 400
```

```gherkin
Funcionalidade: Gestão de fontes e coleções

  Cenário: Listar fontes filtradas por coleção (UC-2)
    Dado que existem 3 fontes na coleção "docs" e 1 na coleção "default"
    Quando eu envio GET /sources com collection "docs"
    Então a resposta tem status 200
    E a lista contém exatamente 3 itens

  Cenário: Remover fonte em cascata (RN-9)
    Dado que existe uma fonte com fileId conhecido e 10 chunks
    Quando eu envio DELETE /sources/{fileId}
    Então a resposta tem status 204
    E os 10 chunks daquela fonte não existem mais no vector store

  Cenário: Remover fonte inexistente
    Quando eu envio DELETE /sources com um fileId aleatório
    Então a resposta tem status 404

  Cenário: Listar coleções com contagens (UC-4)
    Dado que a coleção "docs" tem 3 fontes e 120 chunks
    Quando eu envio GET /collections
    Então a lista contém a coleção "docs" com fileCount 3 e chunkCount 120
```

## 7. Contratos de erro

O corpo de erro segue o schema `Error` do OpenAPI, com um `code` estável (string em maiúsculas) que o frontend pode usar em lógica de tratamento, e uma `message` legível. Os códigos do MVP são: `VALIDATION_ERROR` (400), `SOURCE_FILE_NOT_FOUND` (404 na ingestão), `SOURCE_NOT_FOUND` (404 na remoção), `UNSUPPORTED_TYPE` (415), e `COLLECTION_NOT_FOUND` (422). Mensagens são em português, voltadas ao próprio autor.

## 8. Requisitos não-funcionais verificáveis

A ingestão de um documento típico (até ~50 páginas) deve completar sem timeout da requisição, retornando `202` de forma assíncrona quando o processamento for longo. A latência de query é dominada pela geração em CPU; o critério aceitável no homelab é resposta completa em até poucos segundos com o modelo de geração padrão (`llama3.2:3b-instruct`), sem travar a interface. A reprodutibilidade do chunking (RN-5) deve ser verificável: reingerir o mesmo conteúdo após limpar o índice produz a mesma contagem de chunks.

## 9. Mapeamento spec → implementação

Cada caso de uso mapeia para um controller e um serviço no backend Spring. UC-1, UC-2, UC-3 e UC-4 vivem em torno do pacote `ingestion` e `api`; UC-5 atravessa `retrieval` e `generation`. O contrato OpenAPI pode gerar os DTOs e as interfaces de controller via `openapi-generator`, tornando o código a materialização direta da spec. No frontend, os tipos TypeScript são gerados do mesmo OpenAPI, garantindo que cliente e servidor nunca divirjam. Os cenários Gherkin viram testes de aceitação (por exemplo, com Cucumber no backend ou testes de integração equivalentes), fechando o ciclo: a especificação define os testes, e os testes provam a conformidade da implementação.