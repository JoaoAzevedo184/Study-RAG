package com.joaoazevedo.studyrag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros da recuperação por similaridade (UC-5). No MVP a busca é puramente
 * por similaridade no PgVectorStore — sem hybrid search nem reranking.
 */
@ConfigurationProperties(prefix = "studyrag.retrieval")
public class RetrievalProperties {

    /** Número de chunks recuperados quando o pedido não informa {@code topK} (contrato: 5). */
    private int defaultTopK = 5;

    /**
     * Score mínimo de similaridade (cosseno normalizado, 0..1) para um chunk ser
     * considerado relevante. Chunks abaixo do limiar são descartados; quando nenhum
     * sobra, a query responde "não sei" com {@code sources} vazio (RN-7).
     */
    private double similarityThreshold = 0.5;

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}
