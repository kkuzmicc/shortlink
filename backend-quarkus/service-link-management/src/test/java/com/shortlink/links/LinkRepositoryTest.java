package com.shortlink.links;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LinkRepositoryTest {
    @Inject
    LinkRepository repository;

    @Test
    void persistsSchemaDefaultsAndRecoversAfterCodeCollision() {
        String code = "repo" + UUID.randomUUID().toString().substring(0, 8);
        LinkRepository.Link first = repository.insert(code, "https://example.com", null);

        assertThrows(RuntimeException.class,
                () -> repository.insert(code, "https://example.com/duplicate", null));

        // The failed transaction must not poison subsequent insert attempts.
        LinkRepository.Link second = repository.insert(code + "x", "https://example.com/next", "Next");
        assertNotEquals(first.id(), second.id());
        assertNotNull(first.createdAt());
        assertNull(first.title());

        QuarkusTransaction.requiringNew().run(() -> {
            LinkEntity stored = repository.findById(first.id());
            assertEquals("https://example.com", stored.targetUrl);
            assertEquals("ACTIVE", stored.status);
            assertEquals((short) 302, stored.redirectType);
            assertEquals(Long.valueOf(1), stored.version);
            assertNotNull(stored.updatedAt);
            assertNull(stored.ownerId);
            assertNull(stored.expiresAt);
        });
    }
}
