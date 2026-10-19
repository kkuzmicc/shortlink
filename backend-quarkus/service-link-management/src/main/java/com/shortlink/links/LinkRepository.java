package com.shortlink.links;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LinkRepository {
    @Inject
    DataSource dataSource;

    public Link insert(String code, String targetUrl, String title) throws SQLException {
        String sql = """
                INSERT INTO links (id, code, target_url, title)
                VALUES (?, ?, ?, ?)
                RETURNING id, code, target_url, title, status, created_at
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, UUID.randomUUID());
                statement.setString(2, code);
                statement.setString(3, targetUrl);
                statement.setString(4, title);

                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return map(result);
                }
        }
    }

    public List<Link> list(int limit, int offset) {
        String sql = """
                SELECT id, code, target_url, title, status, created_at
                FROM links ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?""";

        try (Connection connection = dataSource.getConnection();

             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);

            try (ResultSet result = statement.executeQuery()) {
                List<Link> links = new ArrayList<>();

                while (result.next()) {
                    links.add(map(result));
                }
                return links;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not list links", e);
        }
    }

    private Link map(ResultSet result) throws SQLException {
        return new Link(
                result.getObject("id", UUID.class),
                result.getString("code"),
                result.getString("target_url"),
                result.getString("title"),
                result.getString("status"),
                result.getObject("created_at", java.time.OffsetDateTime.class).toInstant());
    }

    public record Link(UUID id, String code, String targetUrl, String title, String status, Instant createdAt) {}
}
