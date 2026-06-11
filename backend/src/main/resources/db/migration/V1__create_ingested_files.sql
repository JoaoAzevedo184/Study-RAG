-- Rastreamento de fontes para deduplicação (RN-2, RN-3) e atualização incremental.
-- A tabela de vetores (vector_store) é gerenciada pelo PgVectorStore do Spring AI,
-- portanto NÃO é criada aqui (ver spring.ai.vectorstore.pgvector.initialize-schema).

CREATE TABLE ingested_files (
    id           UUID PRIMARY KEY,
    source_type  VARCHAR(20)  NOT NULL,                       -- 'pdf' | 'markdown'
    source_uri   TEXT         NOT NULL,
    collection   VARCHAR(80)  NOT NULL DEFAULT 'default',     -- slug do tema (RN-4)
    content_hash CHAR(64)     NOT NULL,                       -- SHA-256 do texto extraído (RN-3)
    title        TEXT,
    chunk_count  INTEGER      NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'INGESTED',    -- INGESTED | PROCESSING | FAILED
    ingested_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_ingested_files_source_uri UNIQUE (source_uri)  -- RN-2: unicidade de fonte
);

-- Acelera a listagem/contagem por coleção (UC-2, UC-4).
CREATE INDEX idx_ingested_files_collection ON ingested_files (collection);
