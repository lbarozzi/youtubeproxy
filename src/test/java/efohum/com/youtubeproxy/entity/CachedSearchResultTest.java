package efohum.com.youtubeproxy.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CachedSearchResultTest {

    @Test
    void testIsExpired_WhenNotExpired() {
        // Arrange
        CachedSearchResult result = new CachedSearchResult();
        result.setCreatedAt(LocalDateTime.now());
        result.setExpiresAt(LocalDateTime.now().plusHours(1));

        // Act
        boolean expired = result.isExpired();

        // Assert
        assertFalse(expired);
    }

    @Test
    void testIsExpired_WhenExpired() {
        // Arrange
        CachedSearchResult result = new CachedSearchResult();
        result.setCreatedAt(LocalDateTime.now().minusDays(2));
        result.setExpiresAt(LocalDateTime.now().minusDays(1));

        // Act
        boolean expired = result.isExpired();

        // Assert
        assertTrue(expired);
    }

    @Test
    void testOnCreate_SetsTimestamps() {
        // Arrange
        CachedSearchResult result = new CachedSearchResult();
        result.setQueryKey("test-key");
        result.setResponseJson("{}");

        // Simulate @PrePersist
        result.onCreate();

        // Assert
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(result.getCreatedAt()));
    }

    @Test
    void testOnCreate_ExpiresAt24Hours() {
        // Arrange
        CachedSearchResult result = new CachedSearchResult();
        result.setQueryKey("test-key");

        // Act - Simulate @PrePersist
        result.onCreate();

        // Assert
        LocalDateTime expectedExpiry = result.getCreatedAt().plusHours(24);
        assertEquals(expectedExpiry, result.getExpiresAt());
    }

    @Test
    void testOnCreate_DoesNotOverrideExistingExpiresAt() {
        // Arrange
        CachedSearchResult result = new CachedSearchResult();
        result.setQueryKey("test-key");
        LocalDateTime customExpiry = LocalDateTime.now().plusHours(48);
        result.setExpiresAt(customExpiry);

        // Act - Simulate @PrePersist
        result.onCreate();

        // Assert
        assertEquals(customExpiry, result.getExpiresAt());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange & Act
        CachedSearchResult result = new CachedSearchResult();
        result.setId(1L);
        result.setQueryKey("test-query-key");
        result.setResponseJson("{\"items\": []}");
        result.setCreatedAt(LocalDateTime.now());
        result.setExpiresAt(LocalDateTime.now().plusDays(1));

        // Assert
        assertEquals(1L, result.getId());
        assertEquals("test-query-key", result.getQueryKey());
        assertEquals("{\"items\": []}", result.getResponseJson());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiresAt());
    }
}
