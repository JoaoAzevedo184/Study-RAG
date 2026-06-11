package com.joaoazevedo.studyrag.ingestion.dedup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Acesso à tabela {@code ingested_files} via {@link JdbcTemplate}. Concentra as
 * consultas usadas pela deduplicação (RN-3), pela listagem de fontes (UC-2) e
 * pela agregação de coleções (UC-4).
 */
@Repository
public class IngestedFileRepository {

    private final JdbcTemplate jdbc;

    public IngestedFileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<IngestedFile> ROW_MAPPER = IngestedFileRepository::mapRow;

    private static IngestedFile mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new IngestedFile(
                rs.getObject("id", UUID.class),
                rs.getString("source_type"),
                rs.getString("source_uri"),
                rs.getString("collection"),
                rs.getString("content_hash"),
                rs.getString("title"),
                rs.getInt("chunk_count"),
                rs.getString("status"),
                rs.getObject("ingested_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    public Optional<IngestedFile> findBySourceUri(String sourceUri) {
        List<IngestedFile> rows = jdbc.query(
                "SELECT * FROM ingested_files WHERE source_uri = ?", ROW_MAPPER, sourceUri);
        return rows.stream().findFirst();
    }

    public Optional<IngestedFile> findById(UUID id) {
        List<IngestedFile> rows = jdbc.query(
                "SELECT * FROM ingested_files WHERE id = ?", ROW_MAPPER, id);
        return rows.stream().findFirst();
    }

    public List<IngestedFile> findAll() {
        return jdbc.query("SELECT * FROM ingested_files ORDER BY ingested_at DESC", ROW_MAPPER);
    }

    public List<IngestedFile> findByCollection(String collection) {
        return jdbc.query(
                "SELECT * FROM ingested_files WHERE collection = ? ORDER BY ingested_at DESC",
                ROW_MAPPER, collection);
    }

    public void insert(IngestedFile file) {
        jdbc.update("""
                INSERT INTO ingested_files
                    (id, source_type, source_uri, collection, content_hash, title,
                     chunk_count, status, ingested_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """,
                file.id(), file.sourceType(), file.sourceUri(), file.collection(),
                file.contentHash(), file.title(), file.chunkCount(), file.status());
    }

    /** Atualiza uma fonte reindexada (RN-3: hash mudou). */
    public void update(UUID id, String collection, String contentHash, String title,
                       int chunkCount, String status) {
        jdbc.update("""
                UPDATE ingested_files
                   SET collection = ?, content_hash = ?, title = ?,
                       chunk_count = ?, status = ?, updated_at = now()
                 WHERE id = ?
                """,
                collection, contentHash, title, chunkCount, status, id);
    }

    public boolean deleteById(UUID id) {
        return jdbc.update("DELETE FROM ingested_files WHERE id = ?", id) > 0;
    }

    public boolean existsByCollection(String collection) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM ingested_files WHERE collection = ?", Integer.class, collection);
        return count != null && count > 0;
    }

    /** Agrega coleções com contagem de fontes e de chunks (UC-4). */
    public List<CollectionAggregate> aggregateCollections() {
        return jdbc.query("""
                SELECT collection,
                       count(*)               AS file_count,
                       coalesce(sum(chunk_count), 0) AS chunk_count
                  FROM ingested_files
                 GROUP BY collection
                 ORDER BY collection
                """,
                (rs, rowNum) -> new CollectionAggregate(
                        rs.getString("collection"),
                        rs.getInt("file_count"),
                        rs.getInt("chunk_count")));
    }

    /** Projeção de agregação por coleção. */
    public record CollectionAggregate(String name, int fileCount, int chunkCount) {
    }
}
