package com.joaoazevedo.studyrag.ingestion.dedup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContentHasherTest {

    private final ContentHasher hasher = new ContentHasher();

    @Test
    void emptyStringHasKnownSha256() {
        assertThat(hasher.sha256(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void hashIsDeterministicAndSensitiveToChange() {
        String a = hasher.sha256("conteúdo do guia");
        String b = hasher.sha256("conteúdo do guia");
        String c = hasher.sha256("conteúdo do guia alterado");
        assertThat(a).isEqualTo(b).hasSize(64);
        assertThat(c).isNotEqualTo(a);
    }
}
