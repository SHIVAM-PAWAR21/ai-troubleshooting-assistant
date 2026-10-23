package org.assignment.troubleshooter.repository;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Reads the knowledge base from MySQL. Written for Spring Boot 4 (Jackson 3, package tools.jackson). */
@Repository
public class KbRepository {

    private static final String SELECT_ALL =
            "SELECT id, title, symptoms, keywords, diagnostic_steps, known_resolution, "
                    + "escalation_team, embedding FROM knowledge_base ORDER BY id";

    private final JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public KbRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<KnowledgeEntry> findAll() {
        return jdbc.query(SELECT_ALL, this::mapRow);
    }

    private KnowledgeEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeEntry(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("symptoms"),
                parseStrings(rs.getString("keywords")),
                parseStrings(rs.getString("diagnostic_steps")),
                rs.getString("known_resolution"),
                rs.getString("escalation_team"),
                parseVector(rs.getString("embedding")));
    }

    private List<String> parseStrings(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid JSON in knowledge_base: " + json, e);
        }
    }

    private double[] parseVector(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, double[].class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid embedding JSON in knowledge_base", e);
        }
    }
}
