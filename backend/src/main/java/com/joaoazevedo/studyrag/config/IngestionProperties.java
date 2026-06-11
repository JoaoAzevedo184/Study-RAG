package com.joaoazevedo.studyrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração portável da ingestão (RN-5: parâmetros de chunking fixos no MVP,
 * mas externalizados para reprodutibilidade verificável e testes).
 */
@ConfigurationProperties(prefix = "studyrag.ingestion")
public class IngestionProperties {

    /** Diretório local que contém os arquivos referenciados por {@code sourceUri}. */
    private String uploadsDir = "./uploads";

    /** Prefixo lógico do {@code sourceUri} mapeado para {@link #uploadsDir} (ex.: {@code /uploads}). */
    private String uriPrefix = "/uploads";

    private final Chunk chunk = new Chunk();

    public String getUploadsDir() {
        return uploadsDir;
    }

    public void setUploadsDir(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }

    public void setUriPrefix(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }

    public Chunk getChunk() {
        return chunk;
    }

    /** Parâmetros do chunking recursivo (RN-5: alvo 1000, overlap 200). */
    public static class Chunk {
        private int size = 1000;
        private int overlap = 200;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getOverlap() {
            return overlap;
        }

        public void setOverlap(int overlap) {
            this.overlap = overlap;
        }
    }
}
