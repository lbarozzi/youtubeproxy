package efohum.com.youtubeproxy.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import efohum.com.youtubeproxy.entity.CachedSearchResult;
import efohum.com.youtubeproxy.entity.CachedVideo;
import efohum.com.youtubeproxy.repository.CachedSearchResultRepository;
import efohum.com.youtubeproxy.repository.CachedVideoRepository;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class YouTubeProxyServiceTest {

    @Mock
    private CachedSearchResultRepository searchResultRepository;

    @Mock
    private CachedVideoRepository videoRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private YouTubeProxyService youTubeProxyService;

    private static final String API_KEY = "test-api-key";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(youTubeProxyService, "apiKey", API_KEY);
        ReflectionTestUtils.setField(youTubeProxyService, "baseUrl", BASE_URL);
    }

    @Test
    void testSearchVideos_CacheHit() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet");
        params.put("q", "spring boot");

        CachedSearchResult cachedResult = new CachedSearchResult();
        cachedResult.setQueryKey("test-key");
        cachedResult.setResponseJson("{\"items\": []}");
        cachedResult.setCreatedAt(LocalDateTime.now());
        cachedResult.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(searchResultRepository.findByQueryKey(anyString()))
            .thenReturn(Optional.of(cachedResult));

        // Act
        String result = youTubeProxyService.searchVideos(params);

        // Assert
        assertNotNull(result);
        assertEquals("{\"items\": []}", result);
        verify(searchResultRepository, times(1)).findByQueryKey(anyString());
        verify(webClientBuilder, never()).baseUrl(anyString());
    }

    @Test
    void testSearchVideos_CacheMiss() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet");
        params.put("q", "java");

        String apiResponse = "{\"items\": [{\"id\": \"123\"}]}";

        when(searchResultRepository.findByQueryKey(anyString()))
            .thenReturn(Optional.empty());
        when(webClientBuilder.baseUrl(BASE_URL)).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(apiResponse));
        when(searchResultRepository.save(any(CachedSearchResult.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = youTubeProxyService.searchVideos(params);

        // Assert
        assertNotNull(result);
        assertEquals(apiResponse, result);
        verify(searchResultRepository, times(1)).save(any(CachedSearchResult.class));
    }

    @Test
    void testSearchVideos_CacheExpired() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet");
        params.put("q", "expired");

        CachedSearchResult expiredResult = new CachedSearchResult();
        expiredResult.setQueryKey("expired-key");
        expiredResult.setResponseJson("{\"old\": \"data\"}");
        expiredResult.setCreatedAt(LocalDateTime.now().minusDays(2));
        expiredResult.setExpiresAt(LocalDateTime.now().minusDays(1));
        expiredResult.setId(1L);

        String newApiResponse = "{\"items\": [{\"id\": \"new\"}]}";

        when(searchResultRepository.findByQueryKey(anyString()))
            .thenReturn(Optional.of(expiredResult));
        when(webClientBuilder.baseUrl(BASE_URL)).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(newApiResponse));
        when(searchResultRepository.save(any(CachedSearchResult.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = youTubeProxyService.searchVideos(params);

        // Assert
        assertNotNull(result);
        assertEquals(newApiResponse, result);
        verify(searchResultRepository, times(1)).save(any(CachedSearchResult.class));
    }

    @Test
    void testGetVideoDetails_CacheHit() {
        // Arrange
        String videoId = "test-video-id";
        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet,statistics");

        CachedVideo cachedVideo = new CachedVideo();
        cachedVideo.setVideoId(videoId);
        cachedVideo.setResponseJson("{\"items\": [{\"id\": \"test-video-id\"}]}");
        cachedVideo.setCreatedAt(LocalDateTime.now());
        cachedVideo.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(videoRepository.findByVideoId(videoId))
            .thenReturn(Optional.of(cachedVideo));

        // Act
        String result = youTubeProxyService.getVideoDetails(videoId, params);

        // Assert
        assertNotNull(result);
        assertEquals("{\"items\": [{\"id\": \"test-video-id\"}]}", result);
        verify(videoRepository, times(1)).findByVideoId(videoId);
        verify(webClientBuilder, never()).baseUrl(anyString());
    }

    @Test
    void testGetVideoDetails_CacheMiss() {
        // Arrange
        String videoId = "new-video-id";
        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet");

        String apiResponse = "{\"items\": [{\"id\": \"new-video-id\", \"snippet\": {\"title\": \"Test\"}, \"statistics\": {\"viewCount\": \"1000\"}}]}";

        when(videoRepository.findByVideoId(videoId))
            .thenReturn(Optional.empty());
        when(webClientBuilder.baseUrl(BASE_URL)).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(apiResponse));
        when(videoRepository.save(any(CachedVideo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = youTubeProxyService.getVideoDetails(videoId, params);

        // Assert
        assertNotNull(result);
        assertEquals(apiResponse, result);
        verify(videoRepository, times(1)).save(any(CachedVideo.class));
    }

    @Test
    void testGetVideoDetails_CacheExpired() {
        // Arrange
        String videoId = "expired-video";
        Map<String, String> params = new HashMap<>();

        CachedVideo expiredVideo = new CachedVideo();
        expiredVideo.setVideoId(videoId);
        expiredVideo.setResponseJson("{\"old\": \"video\"}");
        expiredVideo.setCreatedAt(LocalDateTime.now().minusDays(2));
        expiredVideo.setExpiresAt(LocalDateTime.now().minusDays(1));
        expiredVideo.setId(1L);

        String newApiResponse = "{\"items\": [{\"id\": \"expired-video\", \"snippet\": {}, \"statistics\": {}}]}";

        when(videoRepository.findByVideoId(videoId))
            .thenReturn(Optional.of(expiredVideo));
        when(webClientBuilder.baseUrl(BASE_URL)).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(newApiResponse));
        when(videoRepository.save(any(CachedVideo.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = youTubeProxyService.getVideoDetails(videoId, params);

        // Assert
        assertNotNull(result);
        assertEquals(newApiResponse, result);
        verify(videoRepository, times(1)).save(any(CachedVideo.class));
    }
}
