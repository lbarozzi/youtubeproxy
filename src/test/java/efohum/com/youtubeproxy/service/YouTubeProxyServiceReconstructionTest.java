package efohum.com.youtubeproxy.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import efohum.com.youtubeproxy.entity.CachedSearchResult;
import efohum.com.youtubeproxy.entity.CachedVideo;
import efohum.com.youtubeproxy.repository.CachedSearchResultRepository;
import efohum.com.youtubeproxy.repository.CachedVideoRepository;

@ExtendWith(MockitoExtension.class)
public class YouTubeProxyServiceReconstructionTest {
    
    @Mock
    private CachedSearchResultRepository searchResultRepository;
    
    @Mock
    private CachedVideoRepository videoRepository;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @InjectMocks
    private YouTubeProxyService service;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(service, "apiKey", null); // Simula API key non configurata
    }
    
    @Test
    void testReconstructSearchResponse_WithMatchingVideos() {
        // Given: Video nel DB che matchano la query
        CachedVideo video1 = new CachedVideo();
        video1.setVideoId("video1");
        video1.setTitle("Spring Boot Tutorial");
        video1.setDescription("Learn Spring Boot");
        video1.setChannelId("channel1");
        video1.setChannelTitle("Tech Channel");
        video1.setPublishedAt("2024-01-01T00:00:00Z");
        video1.setThumbnailUrl("https://example.com/thumb1.jpg");
        
        CachedVideo video2 = new CachedVideo();
        video2.setVideoId("video2");
        video2.setTitle("Advanced Spring Concepts");
        video2.setDescription("Deep dive into Spring");
        video2.setChannelId("channel2");
        video2.setChannelTitle("Code Academy");
        video2.setPublishedAt("2024-01-02T00:00:00Z");
        video2.setThumbnailUrl("https://example.com/thumb2.jpg");
        
        when(videoRepository.findAll()).thenReturn(List.of(video1, video2));
        when(searchResultRepository.findByQueryKey(anyString())).thenReturn(Optional.empty());
        
        // When: Cerchiamo "spring"
        Map<String, String> params = new HashMap<>();
        params.put("q", "spring");
        params.put("maxResults", "5");
        
        // Then: Dovrebbe ricostruire una risposta
        String result = service.searchVideos(params);
        assertNotNull(result);
        assertTrue(result.contains("video1"));
        assertTrue(result.contains("video2"));
        assertTrue(result.contains("Spring Boot Tutorial"));
        assertTrue(result.contains("reconstructed-from-db"));
    }
    
    @Test
    void testReconstructSearchResponse_NoMatchingVideos() {
        // Given: Nessun video che matcha la query
        CachedVideo video = new CachedVideo();
        video.setVideoId("video1");
        video.setTitle("Python Tutorial");
        video.setDescription("Learn Python");
        
        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(searchResultRepository.findByQueryKey(anyString())).thenReturn(Optional.empty());
        
        // When: Cerchiamo "java" (non presente)
        Map<String, String> params = new HashMap<>();
        params.put("q", "java");
        params.put("maxResults", "5");
        
        // Then: Dovrebbe lanciare eccezione
        assertThrows(IllegalStateException.class, () -> {
            service.searchVideos(params);
        });
    }
    
    @Test
    void testReconstructSearchResponse_WithExpiredCache() {
        // Given: Cache scaduta - viene restituita direttamente (fallback)
        CachedSearchResult expiredCache = new CachedSearchResult();
        expiredCache.setQueryKey("test-key");
        expiredCache.setResponseJson("{\"kind\":\"youtube#searchListResponse\"}");
        expiredCache.setRegionCode("IT");
        // Simula cache scaduta
        ReflectionTestUtils.setField(expiredCache, "expiresAt", 
            java.time.LocalDateTime.now().minusDays(1));
        
        when(searchResultRepository.findByQueryKey(anyString()))
            .thenReturn(Optional.of(expiredCache));
        
        // When: Cerchiamo con cache scaduta
        Map<String, String> params = new HashMap<>();
        params.put("q", "java");
        params.put("maxResults", "5");
        
        // Then: Dovrebbe restituire la cache scaduta (fallback)
        String result = service.searchVideos(params);
        assertNotNull(result);
        assertEquals(expiredCache.getResponseJson(), result);
    }
    
    @Test
    void testSearchVideosMatchesByDescription() {
        // Given: Video con query nella descrizione
        CachedVideo video = new CachedVideo();
        video.setVideoId("video1");
        video.setTitle("Introduction to Programming");
        video.setDescription("This video teaches Spring Boot framework");
        video.setChannelId("channel1");
        video.setChannelTitle("Learning Channel");
        video.setPublishedAt("2024-01-01T00:00:00Z");
        
        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(searchResultRepository.findByQueryKey(anyString())).thenReturn(Optional.empty());
        
        // When
        Map<String, String> params = new HashMap<>();
        params.put("q", "spring boot");
        params.put("maxResults", "5");
        
        // Then: Dovrebbe trovare il video tramite la descrizione
        String result = service.searchVideos(params);
        assertNotNull(result);
        assertTrue(result.contains("video1"));
        assertTrue(result.contains("Introduction to Programming"));
    }
    
    @Test
    void testSearchVideosMatchesByChannelName() {
        // Given: Video con query nel nome del canale
        CachedVideo video = new CachedVideo();
        video.setVideoId("video1");
        video.setTitle("Daily Vlog");
        video.setDescription("Today's activities");
        video.setChannelId("channel1");
        video.setChannelTitle("Spring Tech Channel");
        video.setPublishedAt("2024-01-01T00:00:00Z");
        
        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(searchResultRepository.findByQueryKey(anyString())).thenReturn(Optional.empty());
        
        // When
        Map<String, String> params = new HashMap<>();
        params.put("q", "spring");
        params.put("maxResults", "5");
        
        // Then: Dovrebbe trovare il video tramite il nome del canale
        String result = service.searchVideos(params);
        assertNotNull(result);
        assertTrue(result.contains("video1"));
        assertTrue(result.contains("Spring Tech Channel"));
    }
    
    @Test
    void testReconstructResponse_LimitsResults() {
        // Given: 10 video nel DB ma maxResults=3
        when(videoRepository.findAll()).thenReturn(List.of(
            createTestVideo("v1", "Spring 1"),
            createTestVideo("v2", "Spring 2"),
            createTestVideo("v3", "Spring 3"),
            createTestVideo("v4", "Spring 4"),
            createTestVideo("v5", "Spring 5")
        ));
        when(searchResultRepository.findByQueryKey(anyString())).thenReturn(Optional.empty());
        
        // When: maxResults=3
        Map<String, String> params = new HashMap<>();
        params.put("q", "spring");
        params.put("maxResults", "3");
        
        // Then: Dovrebbe restituire solo 3 risultati
        String result = service.searchVideos(params);
        assertNotNull(result);
        
        // Conta il numero di videoId nel JSON
        int videoCount = result.split("\"videoId\"").length - 1;
        assertEquals(3, videoCount);
    }
    
    private CachedVideo createTestVideo(String id, String title) {
        CachedVideo video = new CachedVideo();
        video.setVideoId(id);
        video.setTitle(title);
        video.setDescription("Test description");
        video.setChannelId("channel1");
        video.setChannelTitle("Test Channel");
        video.setPublishedAt("2024-01-01T00:00:00Z");
        return video;
    }
}
