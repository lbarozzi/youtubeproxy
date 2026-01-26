package efohum.com.youtubeproxy.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CachedVideoTest {

    @Test
    void testIsExpired_WhenNotExpired() {
        // Arrange
        CachedVideo video = new CachedVideo();
        video.setCreatedAt(LocalDateTime.now());
        video.setExpiresAt(LocalDateTime.now().plusHours(1));

        // Act
        boolean expired = video.isExpired();

        // Assert
        assertFalse(expired);
    }

    @Test
    void testIsExpired_WhenExpired() {
        // Arrange
        CachedVideo video = new CachedVideo();
        video.setCreatedAt(LocalDateTime.now().minusDays(2));
        video.setExpiresAt(LocalDateTime.now().minusDays(1));

        // Act
        boolean expired = video.isExpired();

        // Assert
        assertTrue(expired);
    }

    @Test
    void testIsExpired_JustExpired() {
        // Arrange
        CachedVideo video = new CachedVideo();
        video.setCreatedAt(LocalDateTime.now().minusHours(25));
        video.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        // Act
        boolean expired = video.isExpired();

        // Assert
        assertTrue(expired);
    }

    @Test
    void testOnCreate_SetsTimestamps() {
        // Arrange
        CachedVideo video = new CachedVideo();
        video.setVideoId("test-id");
        video.setResponseJson("{}");

        // Simulate @PrePersist
        video.onCreate();

        // Assert
        assertNotNull(video.getCreatedAt());
        assertNotNull(video.getExpiresAt());
        assertTrue(video.getExpiresAt().isAfter(video.getCreatedAt()));
    }

    @Test
    void testOnCreate_ExpiresAt24Hours() {
        // Arrange
        CachedVideo video = new CachedVideo();
        video.setVideoId("test-id");

        // Act - Simulate @PrePersist
        video.onCreate();

        // Assert
        LocalDateTime expectedExpiry = video.getCreatedAt().plusHours(24);
        assertEquals(expectedExpiry, video.getExpiresAt());
    }

    @Test
    void testSettersAndGetters() {
        // Arrange & Act
        CachedVideo video = new CachedVideo();
        video.setVideoId("video123");
        video.setTitle("Test Video");
        video.setDescription("Test Description");
        video.setChannelId("channel123");
        video.setChannelTitle("Test Channel");
        video.setThumbnailUrl("https://example.com/thumb.jpg");
        video.setViewCount(10000L);
        video.setLikeCount(500L);
        video.setCommentCount(100L);
        video.setFavoriteCount(0L);
        video.setDuration("PT5M30S");
        video.setPublishedAt("2024-01-01T00:00:00Z");
        video.setCategory("10");
        video.setResponseJson("{\"test\": \"data\"}");

        // Assert
        assertEquals("video123", video.getVideoId());
        assertEquals("Test Video", video.getTitle());
        assertEquals("Test Description", video.getDescription());
        assertEquals("channel123", video.getChannelId());
        assertEquals("Test Channel", video.getChannelTitle());
        assertEquals("https://example.com/thumb.jpg", video.getThumbnailUrl());
        assertEquals(10000L, video.getViewCount());
        assertEquals(500L, video.getLikeCount());
        assertEquals(100L, video.getCommentCount());
        assertEquals(0L, video.getFavoriteCount());
        assertEquals("PT5M30S", video.getDuration());
        assertEquals("2024-01-01T00:00:00Z", video.getPublishedAt());
        assertEquals("10", video.getCategory());
        assertEquals("{\"test\": \"data\"}", video.getResponseJson());
    }
}
