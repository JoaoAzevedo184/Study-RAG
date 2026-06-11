package com.joaoazevedo.studyrag.ingestion.dedup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Calcula o SHA-256 do conteúdo de texto extraído (RN-3). O hash é a base da
 * deduplicação: mesmo {@code sourceUri} + mesmo hash ⇒ SKIPPED_UNCHANGED.
 */
@Component
public class ContentHasher {

    /** @return o digest SHA-256 em hexadecimal minúsculo (64 caracteres). */
    public String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido pela plataforma; jamais ocorre.
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
