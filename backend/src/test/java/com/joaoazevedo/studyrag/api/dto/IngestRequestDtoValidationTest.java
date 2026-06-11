package com.joaoazevedo.studyrag.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.joaoazevedo.studyrag.ingestion.SourceType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** RN-4: validação do slug de coleção e obrigatoriedade dos campos. */
class IngestRequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private long violationsOn(IngestRequestDto dto, String field) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .filter(field::equals)
                .count();
    }

    @Test
    void validSlugPasses() {
        var dto = new IngestRequestDto(SourceType.PDF, "/uploads/a.pdf", "bootcamp-ntt", Map.of());
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void nullCollectionIsAllowed() {
        // Coleção ausente recebe default no controller; não deve violar.
        var dto = new IngestRequestDto(SourceType.PDF, "/uploads/a.pdf", null, Map.of());
        assertThat(violationsOn(dto, "collection")).isZero();
    }

    @Test
    void slugWithSpacesAndUppercaseIsRejected() {
        var dto = new IngestRequestDto(SourceType.PDF, "/uploads/a.pdf", "Docs Spring!", Map.of());
        assertThat(violationsOn(dto, "collection")).isPositive();
    }

    @Test
    void slugLongerThan80IsRejected() {
        String tooLong = "a".repeat(81);
        var dto = new IngestRequestDto(SourceType.PDF, "/uploads/a.pdf", tooLong, Map.of());
        assertThat(violationsOn(dto, "collection")).isPositive();
    }

    @Test
    void blankSourceUriIsRejected() {
        var dto = new IngestRequestDto(SourceType.PDF, "  ", "docs", Map.of());
        assertThat(violationsOn(dto, "sourceUri")).isPositive();
    }

    @Test
    void missingSourceTypeIsRejected() {
        Set<ConstraintViolation<IngestRequestDto>> violations =
                validator.validate(new IngestRequestDto(null, "/uploads/a.pdf", "docs", Map.of()));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("sourceType"));
    }
}
