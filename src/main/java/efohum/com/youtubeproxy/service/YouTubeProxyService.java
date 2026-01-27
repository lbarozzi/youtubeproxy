package efohum.com.youtubeproxy.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import efohum.com.youtubeproxy.entity.CachedSearchResult;
import efohum.com.youtubeproxy.entity.CachedVideo;
import efohum.com.youtubeproxy.repository.CachedSearchResultRepository;
import efohum.com.youtubeproxy.repository.CachedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeProxyService {
    
    private final CachedSearchResultRepository searchResultRepository;
    private final CachedVideoRepository videoRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${youtube.api.key}")
    private String apiKey;
    
    @Value("${youtube.api.base-url}")
    private String baseUrl;
    
    /**
     * Verifica se la chiave API è configurata
     */
    private boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("${YOUTUBE_API_KEY}");
    }
    
    /**
     * Cerca video su YouTube con cache
     */
    public String searchVideos(Map<String, String> params) {
        // Genera chiave univoca per la query
        String queryKey = generateQueryKey(params);
        
        // Cerca nel DB
        Optional<CachedSearchResult> cached = searchResultRepository.findByQueryKey(queryKey);
        if (cached.isPresent() && !cached.get().isExpired()) {
            log.info("Cache HIT per search query: {}", queryKey);
            return cached.get().getResponseJson();
        }
        
        // Se la chiave API non è configurata, usa solo il database
        if (!isApiKeyConfigured()) {
            log.warn("API key non configurata. Modalità solo database attiva.");
            if (cached.isPresent()) {
                log.info("Restituisco dati scaduti dalla cache per query: {}", queryKey);
                return cached.get().getResponseJson();
            }
            throw new IllegalStateException("Dati non disponibili nel database e API key non configurata");
        }
        
        // Se non trovato o scaduto, chiama l'API di YouTube
        log.info("Cache MISS per search query: {}, chiamata API YouTube", queryKey);
        String response = callYouTubeSearchApi(params);
        
        // Salva nel DB
        CachedSearchResult newCache = new CachedSearchResult();
        newCache.setQueryKey(queryKey);
        newCache.setResponseJson(response);
        
        if (cached.isPresent()) {
            newCache.setId(cached.get().getId());
        }
        
        searchResultRepository.save(newCache);
        return response;
    }
    
    /**
     * Ottiene dettagli di un video con cache
     */
    public String getVideoDetails(String videoId, Map<String, String> params) {
        // Cerca nel DB
        Optional<CachedVideo> cached = videoRepository.findByVideoId(videoId);
        if (cached.isPresent() && !cached.get().isExpired()) {
            log.info("Cache HIT per video: {}", videoId);
            return cached.get().getResponseJson();
        }
        
        // Se la chiave API non è configurata, usa solo il database
        if (!isApiKeyConfigured()) {
            log.warn("API key non configurata. Modalità solo database attiva.");
            if (cached.isPresent()) {
                log.info("Restituisco dati scaduti dalla cache per video: {}", videoId);
                return cached.get().getResponseJson();
            }
            throw new IllegalStateException("Dati non disponibili nel database e API key non configurata");
        }
        
        // Se non trovato o scaduto, chiama l'API di YouTube
        log.info("Cache MISS per video: {}, chiamata API YouTube", videoId);
        String response = callYouTubeVideosApi(videoId, params);
        
        // Salva nel DB
        CachedVideo newCache = new CachedVideo();
        newCache.setVideoId(videoId);
        newCache.setResponseJson(response);
        // Estrai e salva le statistiche dal JSON
        extractVideoMetadata(response, newCache);
        
        
        if (cached.isPresent()) {
            newCache.setId(cached.get().getId());
        }
        
        videoRepository.save(newCache);
        return response;
    }
    
    /**
     * Chiama l'API search.list di YouTube
     */
    private String callYouTubeSearchApi(Map<String, String> params) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/search");
                    params.forEach(uriBuilder::queryParam);
                    uriBuilder.queryParam("key", apiKey);
                    return uriBuilder.build();
                });
        
        return request.retrieve()
                .bodyToMono(String.class)
                .block();
    }
    
    /**
     * Chiama l'API videos di YouTube
     */
    private String callYouTubeVideosApi(String videoId, Map<String, String> params) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/videos");
                    uriBuilder.queryParam("id", videoId);
                    params.forEach(uriBuilder::queryParam);
                    uriBuilder.queryParam("key", apiKey);
                    return uriBuilder.build();
                });
        
        return request.retrieve()
                .bodyToMono(String.class)
                .block();
    }
    
    /**
     * Genera una chiave univoca per i parametri della query
     */
    private String generateQueryKey(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return sb.toString();
        }
    }
    
    /**
     * Estrae metadati e statistiche dal JSON della risposta YouTube
     */
    private void extractVideoMetadata(String responseJson, CachedVideo video) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode items = root.path("items");
            
            if (items.isArray() && items.size() > 0) {
                JsonNode item = items.get(0);
                
                // Estrai snippet (metadati base)
                JsonNode snippet = item.path("snippet");
                if (!snippet.isMissingNode()) {
                    video.setTitle(snippet.path("title").asText(null));
                    video.setDescription(snippet.path("description").asText(null));
                    video.setChannelId(snippet.path("channelId").asText(null));
                    video.setChannelTitle(snippet.path("channelTitle").asText(null));
                    video.setPublishedAt(snippet.path("publishedAt").asText(null));
                    video.setCategory(snippet.path("categoryId").asText(null));
                    
                    // Estrai thumbnail URL (preferisci alta qualità)
                    JsonNode thumbnails = snippet.path("thumbnails");
                    if (thumbnails.has("high")) {
                        video.setThumbnailUrl(thumbnails.path("high").path("url").asText(null));
                    } else if (thumbnails.has("medium")) {
                        video.setThumbnailUrl(thumbnails.path("medium").path("url").asText(null));
                    } else if (thumbnails.has("default")) {
                        video.setThumbnailUrl(thumbnails.path("default").path("url").asText(null));
                    }
                }
                
                // Estrai statistiche
                JsonNode statistics = item.path("statistics");
                if (!statistics.isMissingNode()) {
                    video.setViewCount(statistics.path("viewCount").asLong(0L));
                    video.setLikeCount(statistics.path("likeCount").asLong(0L));
                    video.setCommentCount(statistics.path("commentCount").asLong(0L));
                    video.setFavoriteCount(statistics.path("favoriteCount").asLong(0L));
                }
                
                // Estrai durata
                JsonNode contentDetails = item.path("contentDetails");
                if (!contentDetails.isMissingNode()) {
                    video.setDuration(contentDetails.path("duration").asText(null));
                }
                
                log.info("Estratte statistiche per video {}: views={}, likes={}", 
                    video.getVideoId(), video.getViewCount(), video.getLikeCount());
            }
        } catch (Exception e) {
            log.error("Errore nell'estrazione dei metadati del video: {}", e.getMessage());
        }
    }
}
