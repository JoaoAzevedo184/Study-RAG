package com.joaoazevedo.studyrag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** RN-1: apenas pdf e markdown são aceitos; outros valores falham (→ 400). */
class SourceTypeTest {

    @Test
    void acceptsSupportedValues() {
        assertThat(SourceType.fromValue("pdf")).isEqualTo(SourceType.PDF);
        assertThat(SourceType.fromValue("markdown")).isEqualTo(SourceType.MARKDOWN);
        assertThat(SourceType.PDF.value()).isEqualTo("pdf");
    }

    @Test
    void rejectsUnsupportedValue() {
        assertThatThrownBy(() -> SourceType.fromValue("docx"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
