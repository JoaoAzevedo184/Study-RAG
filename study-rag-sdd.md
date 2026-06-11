---
documento: Software Design Document (SDD)
projeto: study-rag
versão: 1.1
autor: João Victor Azevedo
data: 2026-06-10
stack_backend: Spring Boot 3.5 + Java 21 + Spring AI + Ollama + pgvector
stack_frontend: React 18 + TypeScript + Vite
status: rascunho
---

# Software Design Document — study-rag

## Sumário

1. Introdução e escopo
2. Visão geral da arquitetura
3. Decisões de stack e justificativas
4. Mapeamento estudo → componente
5. Design do backend
6. Design do frontend
7. Modelagem de dados
8. Contratos de API
9. Empacotamento e deploy
10. Estratégia de avaliação e observabilidade
11. Mapeamento de modelos Ollama
12. Plano de implementação por nível
13. Riscos e mitigações
14. Glossário

---

## 1. Introdução e escopo

### 1.1 Propósito

O **study-rag** é um sistema RAG (Retrieval-Augmented Generation) full-stack que indexa o acervo de estudo pessoal do autor — transcrições de vídeo, PDFs, páginas web salvas e notas do Obsidian — e responde perguntas em linguagem natural com fontes citadas. O sistema serve simultaneamente como consolidação prática do bootcamp NTT DATA Backend Java com Spring AI e como peça de portfólio demonstrando domínio do pipeline RAG completo e de uma aplicação cliente-servidor moderna.

### 1.2 Escopo

Dentro do escopo: ingestão multi-fonte, chunking configurável, embeddings e geração via Ollama local com fallback Groq, busca híbrida, reranking, contextual retrieval, citação de fontes, avaliação automatizada e uma interface web de chat com gestão de fontes. Fora do escopo nesta versão: autenticação multiusuário (sistema single-user no homelab), aplicativo móvel nativo, e fine-tuning de modelos.

### 1.3 Restrições

A restrição dominante é o hardware do homelab: Pop!_OS, 32GB RAM, i3 de 10ª geração, sem GPU dedicada. Isso impõe modelos leves quantizados e descarta LLMs grandes rodando localmente. A segunda restrição é ser single-developer, favorecendo soluções pragmáticas e de baixo atrito operacional sobre arquiteturas distribuídas complexas.

## 2. Visão geral da arquitetura

O sistema adota uma arquitetura cliente-servidor de três camadas físicas: um SPA React no navegador, uma API REST Spring Boot, e a camada de dados/modelos composta por PostgreSQL com pgvector e o servidor Ollama. Toda a stack roda containerizada via Docker Compose no homelab.

```
┌────────────────────────────────────────────────────────────┐
│  NAVEGADOR                                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  React 18 + TypeScript (SPA)                           │  │
│  │  Chat · Gestão de Fontes · Dashboard de Avaliação      │  │
│  └───────────────────────┬──────────────────────────────┘  │
└──────────────────────────┼──────────────────────────────────┘
                           │ HTTP/JSON (+ SSE para streaming)
┌──────────────────────────▼──────────────────────────────────┐
│  SPRING BOOT 3.5 (API REST) — Java 21 + Virtual Threads      │
│  ┌─────────┐  ┌──────────────┐  ┌─────────────┐  ┌────────┐  │
│  │Ingestão │  │Retrieval     │  │Geração      │  │Avaliação│ │
│  │ETL      │  │Híbrido+Rerank│  │QA Advisor   │  │RAGAS    │ │
│  └────┬────┘  └──────┬───────┘  └─────┬───────┘  └────────┘  │
│       │              │                │      + Actuator/Micrometer│
└───────┼──────────────┼────────────────┼─────────────────────┘
        │              │                │
   ┌────▼──────────────▼────┐      ┌────▼──────────┐
   │ PostgreSQL + pgvector  │      │   Ollama      │
   │ vetores + FTS + dados  │      │ embed + LLM   │
   └────────────────────────┘      │ (fallback Groq)│
                                    └───────────────┘
        │ Prometheus scrape
   ┌────▼─────┐
   │ Grafana  │
   └──────────┘
```

O frontend nunca fala diretamente com Ollama ou com o banco; todo o pipeline RAG permanece no backend, mantendo a chave Groq e a lógica de retrieval do lado servidor.

## 3. Decisões de stack e justificativas

### 3.1 Backend: Spring AI

A escolha do Spring AI sobre LangChain (Python) ou frameworks Go se justifica pelo menor atrito com o ecossistema que o autor já estuda, pela abstração de serviço portável que permite trocar Ollama ↔ Groq alterando apenas o `application.yml` sem tocar no código Java, pela integração nativa com Spring Boot Actuator para métricas automáticas de tokens e latência, e pelo uso de Virtual Threads do Java 21 que escalam bem as chamadas de I/O bloqueante ao Ollama. Usar LangChain exigiria manter infraestrutura Python paralela, e frameworks Go ainda têm ecossistema de orquestração imaturo.

### 3.2 Banco vetorial: pgvector

O pgvector é "dois em um": armazena dados relacionais (rastreamento de fontes, metadados) e vetores no mesmo PostgreSQL, simplificando a infraestrutura. A integração com Spring AI via `PgVectorStore` é madura e similar ao `JdbcTemplate`, e o Spring Boot pode subir o banco automaticamente via Docker Compose. Qdrant permanece como alternativa plugável caso a base cresça e a quantização agressiva passe a valer a pena — troca que custa apenas mudar o starter, graças à abstração portável.

### 3.3 Modelos: Ollama local

Seguindo as restrições de hardware, `nomic-embed-text` (768 dimensões) é o embedding padrão pelo equilíbrio qualidade/velocidade em CPU — nenhum dos LLMs de chat instalados serve como modelo de embedding, então este precisa ser baixado à parte. Para geração, o `llama3.2:3b-instruct-q4_K_M` (2 GB) é o LLM padrão por equilibrar qualidade e latência em CPU; modelos sub-1GB cobrem tarefas auxiliares rápidas (query rewriting, multi-query), e o fallback Groq permanece alternável por configuração quando qualidade ou latência forem críticas. Os modelos grandes (17 GB+) instalados no host só são viáveis com GPU e ficam fora do caminho crítico do RAG. O detalhamento completo está na Seção 11. O número de threads do Ollama deve ser maximizado via `spring.ai.ollama.chat.options.num-thread`.

### 3.4 Frontend: React + TypeScript + Vite

React com TypeScript foi a stack escolhida. Vite é recomendado como bundler pela velocidade de dev server e build, superior ao Create React App (descontinuado). TypeScript dá segurança de tipos nos contratos de API, especialmente úteis num app que troca payloads estruturados (respostas com fontes e métricas). Para estado de servidor e cache de requisições, **TanStack Query** (React Query) é recomendado; para estilização, **Tailwind CSS** pela rapidez de prototipagem.

### 3.5 Empacotamento recomendado

A recomendação é **container separado** no Docker Compose, e não servir o frontend pelo Spring Boot. Razões: build do frontend (Vite) e do backend (Maven) ficam independentes e mais rápidos; o frontend pode ser servido por Nginx com cache e compressão adequados; e a separação reflete uma arquitetura realista de portfólio. Nginx também atua como reverse proxy, roteando `/api` para o backend e servindo os assets estáticos, evitando problemas de CORS.

## 4. Mapeamento estudo → componente

Cada conceito estudado no notebook de RAG vira um elemento concreto do sistema.

| Conceito estudado | Componente no study-rag |
| :--- | :--- |
| Arquitetura RAG completa | Pacotes `ingestion → embedding → retrieval → generation` |
| Estratégias de chunking | `ChunkingStrategy` plugável (recursivo padrão, semântico opt-in) |
| Falhas do naive RAG | `QueryTransformer` (rewriting, multi-query) pré-retrieval |
| Spring AI vs alternativas | Stack backend confirmada |
| Ollama embeddings + LLM | `OllamaEmbeddingModel` + `OllamaChatModel` configuráveis |
| Bancos vetoriais | `PgVectorStore` padrão, Qdrant plugável |
| Avaliação (RAGAS, métricas) | Módulo de avaliação + dashboard no frontend |
| Ingestão multi-fonte | `DocumentReader` por tipo + tabela `ingested_files` |
| Web scraping de docs (inspiração Docstóteles) | `SiteDocReader` — crawl de site inteiro via Fire Crawl |
| Isolamento por tema (coleções) | Campo `collection` nos metadados, filtrável no retrieval |
| Hybrid search | Retrieval pgvector denso + Full-Text Search do Postgres |
| RAG avançado | Contextual retrieval (Nível 2) + agentic RAG (Nível 4) |

## 5. Design do backend

### 5.1 Estrutura de pacotes

A organização segue o pipeline RAG, com cada camada isolada e testável de forma independente.

```
com.joaoazevedo.studyrag
├── config          # Beans de ChatClient, VectorStore, configuração portável
├── ingestion       # Readers (PDF, web, vídeo, markdown), splitters, ETL
│   ├── reader
│   ├── chunking    # ChunkingStrategy (recursive, semantic)
│   └── dedup       # Hashing e tabela ingested_files
├── retrieval       # QueryTransformer, hybrid search, reranking
├── generation      # QuestionAnswerAdvisor, montagem de citações
├── evaluation      # Geração de dataset sintético, RAGAS-style, evaluators
├── api             # Controllers REST + DTOs
└── observability   # Métricas customizadas Micrometer
```

### 5.2 Camada de ingestão

A ingestão implementa o padrão ETL do Spring AI (Reader → Transformer → Writer), normalizando fontes heterogêneas em um formato comum de documento (texto + metadados). PDFs usam `TikaDocumentReader` pela versatilidade; páginas web isoladas usam `JsoupDocumentReader` extraindo o conteúdo principal e descartando scripts e estilos; transcrições de vídeo passam por limpeza de vícios de oralidade antes do splitting; notas Markdown do Obsidian usam parsing por estrutura, respeitando cabeçalhos.

Um reader adicional, o `SiteDocReader`, cobre o caso de **documentação inteira de uma tecnologia** — inspirado no projeto Docstóteles. Em vez de ingerir uma página por vez, ele recebe a URL raiz de uma doc (Spring, Django, React etc.), descobre todas as URLs do site via crawling (Fire Crawl: `map_url` + `batch_scrape_urls`) e recebe de volta o conteúdo já convertido em Markdown limpo, pronto para chunking. Fire Crawl tem tier gratuito e acelera a validação; como é serviço externo com chave de API, fica encapsulado atrás de uma interface `SiteCrawler` que permite trocar por uma alternativa self-hosted (Crawl4AI ou crawler Jsoup próprio) sem afetar o resto do pipeline, preservando o caráter offline-first do projeto.

Toda fonte é associada a uma **coleção** — um agrupamento nomeado por tema (ex: "docs-spring", "transcricoes-bootcamp"). A coleção é gravada nos metadados JSONB de cada chunk, permitindo filtrar o retrieval por tema e evitando que uma pergunta sobre Spring traga ruído de uma doc de Django. Isso supera o modelo do Docstóteles, onde cada coleção era um índice FAISS em memória recriado a cada pergunta; aqui as coleções coexistem persistidas no mesmo pgvector e são selecionadas por filtro de metadados.

A deduplicação registra o hash SHA-256 do conteúdo extraído na tabela `ingested_files`; se o hash não mudou, a ingestão é pulada. A atualização incremental usa lógica de upsert e, ao receber nova versão de uma fonte, deleta os vetores antigos filtrando por `source_uri` antes de inserir os novos.

### 5.3 Camada de retrieval

O retrieval pode opcionalmente transformar a consulta antes de buscar, combatendo as falhas do naive RAG: query rewriting limpa perguntas prolixas, e multi-query gera variações semânticas para melhorar o recall. A busca é híbrida: a parte densa usa o índice HNSW do pgvector com distância de cosseno, e a parte por palavra-chave usa o Full-Text Search nativo do Postgres, fundidas por pesos no backend. O reranking com cross-encoder reordena os candidatos recuperados, reduzindo o overflow de contexto ao enviar ao LLM apenas os chunks mais pertinentes.

### 5.4 Camada de geração

A geração usa o `QuestionAnswerAdvisor` do Spring AI, que encapsula a recuperação e o aumento do prompt. O prompt instrui o modelo a responder exclusivamente com base no contexto fornecido e a referenciar de qual chunk veio cada afirmação, possibilitando a citação de fontes na resposta. Para a experiência de chat, a geração suporta streaming via Server-Sent Events, permitindo que o frontend renderize a resposta token a token.

## 6. Design do frontend

### 6.1 Estrutura do projeto

```
src/
├── api/            # Cliente HTTP tipado, hooks TanStack Query
│   ├── client.ts   # axios/fetch wrapper com baseURL /api
│   └── types.ts    # Tipos espelhando os DTOs do backend
├── components/     # Componentes reutilizáveis (UI)
├── features/
│   ├── chat/       # Interface de pergunta-resposta com fontes
│   ├── sources/    # Upload e gestão de fontes ingeridas
│   └── evaluation/ # Dashboard de métricas de qualidade
├── hooks/          # Hooks customizados
├── lib/            # Utilitários
└── App.tsx         # Roteamento (React Router)
```

### 6.2 Telas principais

A **tela de Chat** é o coração da aplicação: um seletor de coleção no topo (para escopar as perguntas a um tema), campo de pergunta, histórico da conversa, e — diferencial deste projeto — cada resposta exibe um painel de fontes citadas expansível, mostrando o trecho original, o documento de origem, a página/seção e o score de relevância. A resposta renderiza via streaming SSE. Toggles permitem ativar/desativar hybrid search e reranking, expondo visualmente o impacto das técnicas estudadas.

A **tela de Gestão de Fontes** organiza os documentos por coleção, lista cada fonte com tipo, título, número de chunks e data, permite upload de novos PDFs, cadastro de URLs isoladas e — inspirado no Docstóteles — o crawl de uma documentação inteira informando a URL raiz e o nome da coleção. Mostra o status de ingestão (ingerido, pulado por hash inalterado, em processamento, crawling) e reflete as tabelas `ingested_files` e a visão de coleções do backend.

A **tela de Dashboard de Avaliação** materializa visualmente o módulo de avaliação: gráficos de faithfulness, answer relevancy, context precision e recall ao longo das execuções, permitindo comparar o efeito de mudanças de chunking ou modelo. Pode embutir ou linkar os painéis do Grafana.

### 6.3 Comunicação com o backend

Toda comunicação passa por um cliente HTTP tipado, com os tipos TypeScript espelhando os DTOs Java para garantir contratos consistentes. O TanStack Query gerencia cache, estados de loading e revalidação. As respostas de chat usam `EventSource` para consumir o stream SSE. O Nginx faz proxy de `/api` para o backend, eliminando CORS em produção; em desenvolvimento, o proxy do Vite cumpre o mesmo papel.

## 7. Modelagem de dados

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Rastreamento de fontes (dedup + atualização incremental)
CREATE TABLE ingested_files (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_type  VARCHAR(20) NOT NULL,   -- 'pdf'|'video'|'web'|'markdown'|'site'
    source_uri   TEXT NOT NULL,
    collection   VARCHAR(80) NOT NULL DEFAULT 'default',  -- agrupamento por tema
    content_hash CHAR(64) NOT NULL,      -- SHA-256 do conteúdo extraído
    title        TEXT,
    chunk_count  INTEGER DEFAULT 0,
    status       VARCHAR(20) DEFAULT 'INGESTED',
    ingested_at  TIMESTAMPTZ DEFAULT now(),
    updated_at   TIMESTAMPTZ DEFAULT now(),
    UNIQUE (source_uri)
);
CREATE INDEX ON ingested_files (collection);

-- Vetores (gerenciada pelo PgVectorStore)
CREATE TABLE vector_store (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content   TEXT NOT NULL,
    metadata  JSONB,                     -- source_uri, collection, page, section
    embedding VECTOR(768),               -- nomic-embed-text
    fts       TSVECTOR
);

CREATE INDEX ON vector_store USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON vector_store USING gin (fts);

-- Histórico de avaliações (alimenta o dashboard do frontend)
CREATE TABLE evaluation_runs (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    run_at           TIMESTAMPTZ DEFAULT now(),
    config_label     TEXT,               -- ex: "recursive-512 / mistral-7b"
    faithfulness     NUMERIC(4,3),
    answer_relevancy NUMERIC(4,3),
    context_precision NUMERIC(4,3),
    context_recall   NUMERIC(4,3),
    sample_size      INTEGER
);
```

As migrations são versionadas com **Flyway**, mantendo coerência com a prática já adotada em projetos anteriores do autor.

## 8. Contratos de API

```http
POST /api/v1/ingest
{ "sourceType": "pdf", "sourceUri": "/uploads/guia.pdf",
  "collection": "bootcamp-ntt",
  "metadata": { "curso": "NTT DATA" } }
→ 202 { "fileId": "uuid", "status": "INGESTED", "chunkCount": 42 }

POST /api/v1/ingest/site
{ "rootUrl": "https://docs.spring.io/spring-ai",
  "collection": "docs-spring", "maxPages": 50 }
→ 202 { "collection": "docs-spring", "status": "CRAWLING",
        "pagesQueued": 50 }

GET /api/v1/collections
→ 200 [ { "name": "docs-spring", "fileCount": 50, "chunkCount": 1240 } ]

GET /api/v1/sources
→ 200 [ { "fileId": "uuid", "sourceType": "pdf", "title": "...",
          "collection": "bootcamp-ntt", "chunkCount": 42,
          "status": "INGESTED", "ingestedAt": "..." } ]

DELETE /api/v1/sources/{fileId}
→ 204

POST /api/v1/query
{ "question": "...", "collection": "docs-spring", "topK": 5,
  "useHybridSearch": true, "useReranking": true }
→ 200 { "answer": "...",
        "sources": [ { "sourceUri": "...", "title": "...", "page": 12,
                       "snippet": "...", "score": 0.89 } ],
        "metrics": { "retrievalMs": 34, "generationMs": 1420,
                     "tokensUsed": 1830 } }

GET /api/v1/query/stream?question=...   (Server-Sent Events)
→ stream de tokens + evento final com sources e metrics

POST /api/v1/evaluation/run
{ "configLabel": "recursive-512 / mistral-7b" }
→ 202 { "runId": "uuid", "status": "RUNNING" }

GET /api/v1/evaluation/runs
→ 200 [ { "runId": "uuid", "faithfulness": 0.91, ... } ]
```

## 9. Empacotamento e deploy

A stack completa sobe via Docker Compose com quatro serviços: o frontend (Nginx servindo o build Vite e fazendo proxy de `/api`), o backend (Spring Boot), o PostgreSQL com pgvector, e o Ollama. Prometheus e Grafana podem reutilizar as instâncias já existentes no homelab.

```yaml
# compose.yaml (esboço)
services:
  frontend:
    build: ./frontend
    ports: ["80:80"]          # Nginx serve SPA + proxy /api
    depends_on: [backend]
  backend:
    build: ./backend
    environment:
      SPRING_AI_OLLAMA_BASE_URL: http://ollama:11434
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/studyrag
    depends_on: [postgres, ollama]
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: studyrag
    volumes: ["pgdata:/var/lib/postgresql/data"]
  ollama:
    image: ollama/ollama
    volumes: ["ollama:/root/.ollama"]
volumes: { pgdata: {}, ollama: {} }
```

O Nginx do frontend roteia requisições `/api/*` para o serviço backend e serve os assets estáticos para todo o resto, eliminando CORS e centralizando a porta de entrada.

## 10. Estratégia de avaliação e observabilidade

A avaliação enfrenta o fato de que falhas de RAG são silenciosas — o modelo responde errado com confiança. O processo gera um dataset sintético de Q&A a partir dos próprios documentos (5 a 15 perguntas por seção, usando um LLM potente para criar perguntas e ground truth), e mede quatro métricas: faithfulness (a resposta se fundamenta só no contexto recuperado), answer relevancy (a resposta atende à pergunta), context precision (os chunks recuperados são úteis) e context recall (todos os chunks necessários foram encontrados). Os `RelevancyEvaluator` e `FactCheckingEvaluator` do Spring AI rodam nos testes, e a avaliação completa executa no CI via GitHub Actions a cada mudança de estratégia.

A observabilidade reaproveita o stack Prometheus/Grafana do homelab. O Actuator e o Micrometer expõem latência de retrieval, latência de geração, tokens consumidos e hit rate, alimentando painéis Grafana — alguns dos quais são embutidos ou linkados no dashboard do frontend.

## 11. Mapeamento de modelos Ollama

O host tem um parque grande de modelos instalados, mas o gargalo é claro: 32GB de RAM sem GPU. Modelos cujos pesos passam de ~7GB deixam pouca folga para o sistema e tornam a inferência em CPU lenta demais para o caminho interativo do RAG. A tabela classifica os modelos disponíveis por viabilidade e papel no projeto.

| Modelo | Tamanho | Viável em 32GB/CPU | Papel no study-rag |
| :--- | :--- | :--- | :--- |
| `nomic-embed-text` (a instalar) | ~274 MB | ✅ Sim | **Embedding padrão** (768 dim) para ingestão e retrieval |
| `llama3.2:1b-instruct-q4_K_M` | 807 MB | ✅ Sim | Tarefas auxiliares rápidas: query rewriting, multi-query |
| `fast` / `balanced` | ~0.8–1 GB | ✅ Sim | Tarefas leves de classificação e transformação de consulta |
| `lfm2.5-thinking` | 731 MB | ✅ Sim | Raciocínio leve, decomposição de perguntas (agentic, Nível 4) |
| `qwen2.5-coder:1.5b-base` | 986 MB | ✅ Sim | Perguntas sobre código; é `base`, melhor para completion |
| `llama3.2:3b-instruct-q4_K_M` | 2.0 GB | ✅ Sim | **LLM de geração padrão do RAG** — melhor qualidade/latência |
| `glm-ocr:bf16` | 2.2 GB | ✅ Sim | OCR de PDFs escaneados na ingestão (complementa o Tika) |
| `qwen3.5:9b` | 6.6 GB | ⚠️ Limítrofe | Geração de maior qualidade em batch; **gerar dataset de avaliação** (Nível 3) |
| `gemma4:26b` · `qwen3.5:27b` · `granite4.1:30b` · `gemma4:31b` | 17–19 GB | ❌ Não | Reservados para quando houver GPU; fora do caminho crítico |
| `laguna-xs.2` · `qwen3.6:35b` | 23 GB | ❌ Não | Inviáveis em CPU — consomem quase toda a RAM |

A configuração portável do Spring AI permite expressar esses papéis como perfis: um modelo de geração padrão, um modelo auxiliar leve para transformação de consulta, e o fallback Groq, todos alternáveis via `application.yml` sem mudança de código. Os modelos grandes permanecem instalados, mas não são referenciados pelo pipeline interativo.

## 12. Plano de implementação por nível

O roadmap segue o formato por níveis dos workshops do autor, com cada nível entregável independentemente.

**Nível 1 — MVP funcional.** Backend: ingestão de PDF e Markdown, chunking recursivo, embeddings nomic-embed-text, pgvector, retrieval por similaridade pura, geração com citação de fontes, dedup por hash, coleções. Frontend: tela de chat básica com fontes e seletor de coleção, tela de upload. Equivale ao naive RAG, mas já com citações.

**Nível 2 — Qualidade de retrieval e web scraping.** Backend: hybrid search, reranking, contextual retrieval estilo Anthropic, `SiteDocReader` com Fire Crawl para ingerir documentação inteira, ingestão de vídeo, streaming SSE. Frontend: crawl de site por URL raiz, toggles de hybrid/reranking, renderização em streaming, painel de fontes enriquecido. Base mínima para produção.

**Nível 3 — Avaliação e observabilidade.** Backend: geração de dataset sintético, métricas RAGAS-style, evaluators no CI, métricas Micrometer. Frontend: dashboard de avaliação com gráficos comparativos entre execuções.

**Nível 4 — Agentic RAG e automação.** Backend: agente com autorreflexão, query rewriting em loop, decomposição de perguntas complexas, sincronização incremental do vault Obsidian via watcher (possivelmente orquestrada por n8n). Frontend: visualização dos passos do agente (qual ferramenta usou, quantas buscas fez). Encaixa no Nível 4 do linux-ai-workshop.

## 13. Riscos e mitigações

O risco dominante é a latência de geração em CPU sem GPU; a mitigação é usar LLMs pequenos quantizados para a maioria das consultas e o fallback Groq apenas quando a qualidade for crítica, alternável por configuração. A dependência do Fire Crawl introduz um serviço externo com chave de API e limite de créditos no tier gratuito; a mitigação é encapsulá-lo atrás da interface `SiteCrawler`, permitindo trocar por um crawler self-hosted sem afetar o pipeline. A qualidade do chunking em transcrições de vídeo é ameaçada por vícios de oralidade; a mitigação é uma etapa de limpeza antes do splitting, possivelmente reaproveitando o skill de transcrição de aula do autor. O índice de Full-Text Search precisa ser reconstruído ao adicionar documentos (limitação inerente ao BM25), exigindo atenção na sincronização incremental do hybrid search. No frontend, o streaming SSE exige tratamento cuidadoso de reconexão e de erros parciais para não deixar respostas truncadas na tela.

## 14. Glossário

**RAG** — Retrieval-Augmented Generation, geração aumentada por recuperação. **Chunk** — fragmento de texto resultante do splitting de um documento. **Embedding** — vetor numérico que representa o significado semântico de um texto. **Hybrid search** — busca que combina vetores densos (semântica) e palavra-chave (BM25/FTS). **Reranking** — reordenação dos candidatos recuperados por um cross-encoder. **Contextual retrieval** — técnica que prefixa contexto global do documento em cada chunk antes do embedding. **Agentic RAG** — RAG com um agente que decide, avalia e reformula buscas em loop. **Coleção** — agrupamento nomeado de fontes por tema, usado para escopar ingestão e retrieval. **Web scraping / crawling** — extração automatizada do conteúdo de um site, aqui usada para ingerir documentação inteira. **Faithfulness** — métrica que mede se a resposta se baseia só no contexto recuperado. **SSE** — Server-Sent Events, canal unidirecional servidor→cliente para streaming. **SPA** — Single Page Application.