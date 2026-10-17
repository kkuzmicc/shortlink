package com.shortlink.links;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
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
