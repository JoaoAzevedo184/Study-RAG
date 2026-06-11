package com.joaoazevedo.studyrag.ingestion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.joaoazevedo.studyrag.config.IngestionProperties;
import com.joaoazevedo.studyrag.error.ApiException;

/**
 * Resolve um {@code sourceUri} lógico (ex.: {@code /uploads/guia.pdf}) para um
 * caminho real sob o diretório de uploads configurado, impedindo path traversal
 * para fora desse diretório.
 */
@Component
public class UploadPathResolver {

    private final Path uploadsRoot;
    private final String uriPrefix;

    public UploadPathResolver(IngestionProperties properties) {
        this.uploadsRoot = Paths.get(properties.getUploadsDir()).toAbsolutePath().normalize();
        this.uriPrefix = properties.getUriPrefix();
    }

    /**
     * @return o caminho real e existente do arquivo apontado por {@code sourceUri}.
     * @throws ApiException SOURCE_FILE_NOT_FOUND (404) se o arquivo não existir;
     *                      VALIDATION_ERROR (400) se o caminho escapar do diretório de uploads.
     */
    public Path resolveExisting(String sourceUri) {
        String relative = sourceUri;
        if (uriPrefix != null && !uriPrefix.isEmpty() && relative.startsWith(uriPrefix)) {
            relative = relative.substring(uriPrefix.length());
        }
        // Remove barras iniciais para forçar resolução relativa ao uploadsRoot.
        relative = relative.replaceFirst("^/+", "");

        Path resolved = uploadsRoot.resolve(relative).normalize();
        if (!resolved.startsWith(uploadsRoot)) {
            throw ApiException.validation("sourceUri aponta para fora do diretório de uploads: " + sourceUri);
        }
        if (!Files.isRegularFile(resolved)) {
            throw ApiException.sourceFileNotFound(sourceUri);
        }
        return resolved;
    }
}
