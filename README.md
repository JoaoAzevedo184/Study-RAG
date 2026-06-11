# study-rag

Sistema RAG (Retrieval-Augmented Generation) full-stack e single-user que indexa
um acervo de estudo pessoal (PDFs, Markdown) e responde perguntas em linguagem
natural **com fontes citadas**. Serve como consolidação prática do bootcamp NTT
DATA Backend Java com Spring AI e como peça de portfólio do pipeline RAG completo.

O contrato [`study-rag-openapi.yml`](study-rag-openapi.yml) é a **fonte de verdade
única** dos endpoints; backend, frontend e testes derivam dele.

## Stack

| Camada | Tecnologia |
| :--- | :--- |
| Backend | Spring Boot 3.5 · Java 21 (Virtual Threads) · Spring AI |
| Dados | PostgreSQL + pgvector (vetores + relacional) |
| Modelos | Ollama local (`nomic-embed-text`, `llama3.2:3b-instruct`) · fallback Groq |
| Frontend | React 18 · TypeScript · Vite *(em construção)* |
| Migrations | Flyway |
| Observabilidade | Actuator · Micrometer · Prometheus · Grafana |

## Estrutura do repositório

```
study-rag/
├── study-rag-openapi.yml   # Contrato OpenAPI — fonte de verdade
├── openapitools.json       # Config do openapi-generator
├── docs/                   # SDD, spec MVP, integração entre agentes
├── backend/                # API Spring Boot (este módulo)
├── frontend/               # SPA React (em construção)
└── generated/              # DTOs/clientes gerados do contrato (backend + frontend)
```

## Como rodar o backend

Pré-requisitos: JDK 21, PostgreSQL com extensão `vector`, Ollama com os modelos
`nomic-embed-text` e `llama3.2:3b-instruct-q4_K_M` baixados.

```bash
cd backend
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080/api/v1`. Configuração em
[`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)
(datasource, Ollama, pgvector, diretório de uploads).

### Testes

```bash
cd backend
./mvnw verify
```

## Documentação

| Documento | Conteúdo |
| :--- | :--- |
| [docs/SDD.md](docs/SDD.md) | Software Design Document — arquitetura, decisões de stack, modelos |
| [docs/SDD-MVP.md](docs/SDD-MVP.md) | Spec-Driven (Nível 1) — regras de negócio, casos de uso, critérios de aceite |
| [docs/INTEGRATION.md](docs/INTEGRATION.md) | Pontos de acoplamento para os pacotes `retrieval` / `generation` |

## Roadmap por nível

- **Nível 1 — MVP** *(em andamento)*: ingestão PDF/Markdown, dedup por hash,
  coleções, chunking recursivo, embeddings Ollama, pgvector, retrieval por
  similaridade, geração com citação. Ingestão e gestão de fontes (`UC-1..UC-4`)
  implementadas; `UC-5` (query) é o próximo passo.
- **Nível 2**: hybrid search, reranking, contextual retrieval, web scraping, SSE.
- **Nível 3**: avaliação RAGAS-style, métricas Micrometer, dashboard.
- **Nível 4**: agentic RAG, sincronização incremental do vault Obsidian.

## Licença

Projeto pessoal de estudo. © João Victor Azevedo.