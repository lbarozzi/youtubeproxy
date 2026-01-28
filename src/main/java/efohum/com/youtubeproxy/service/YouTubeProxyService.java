package efohum.com.youtubeproxy.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    
    @Value("${api.youtube.proxyonly:false}")
    private boolean proxyOnlyMode;
    
    /**
     * Verifica se la chiave API è configurata e il proxy-only mode è disabilitato
     */
    private boolean isApiKeyConfigured() {
        // Se proxy-only mode è attivo, comportati come se non ci fosse API key
        if (proxyOnlyMode) {
            log.debug("Proxy-only mode attivo: uso solo cache/database");
            return false;
        }
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
            
            // Prova a ricostruire una risposta parziale dai video salvati nel DB
            log.info("Tentativo di ricostruzione risposta dai video nel database per query: {}", queryKey);
            String reconstructedResponse = reconstructSearchResponse(params, cached.orElse(null));
            if (reconstructedResponse != null) {
                log.info("Risposta ricostruita con successo da {} video nel database", 
                    countVideosInResponse(reconstructedResponse));
                return reconstructedResponse;
            }
            
            // Se non ci sono dati, restituisci una risposta vuota valida invece di errore
            log.warn("Nessun dato disponibile nel database per query: {}. Restituisco risposta vuota.", queryKey);
            return createEmptySearchResponse();
        }
        
        // Se non trovato o scaduto, chiama l'API di YouTube
        log.info("Cache MISS per search query: {}, chiamata API YouTube", queryKey);
        String response = callYouTubeSearchApi(params);
        
        // Salva nel DB
        CachedSearchResult newCache = new CachedSearchResult();
        newCache.setQueryKey(queryKey);
        newCache.setResponseJson(response);
        
        // Estrai e salva metadati dalla risposta
        extractAndSaveSearchMetadata(response, params, newCache);
        
        boolean isUpdate = cached.isPresent();
        if (isUpdate) {
            newCache.setId(cached.get().getId());
            log.info("Aggiornamento cache search esistente: queryKey={}, query='{}'", 
                queryKey, newCache.getQuery());
        } else {
            log.info("Nuova cache search: queryKey={}, query='{}', maxResults={}", 
                queryKey, newCache.getQuery(), newCache.getMaxResults());
        }
        
        searchResultRepository.save(newCache);
        log.info("Cache search salvata con successo: id={}, query='{}', totalResults={}",
            newCache.getId(), newCache.getQuery(), newCache.getTotalResults());
        
        // Estrai e salva i singoli video dalla risposta
        extractAndSaveVideosFromSearch(response);
        
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
            log.warn("Video {} non disponibile nel database. Restituisco risposta vuota.", videoId);
            return createEmptyVideoResponse();
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
        
        boolean isUpdate = cached.isPresent();
        if (isUpdate) {
            newCache.setId(cached.get().getId());
            log.info("Aggiornamento cache video esistente: videoId={}, title='{}'", 
                videoId, newCache.getTitle());
        } else {
            log.info("Nuova cache video: videoId={}, title='{}'", 
                videoId, newCache.getTitle());
        }
        
        videoRepository.save(newCache);
        log.info("Cache video salvata: id={}, videoId={}, title='{}', views={}, likes={}",
            newCache.getId(), newCache.getVideoId(), newCache.getTitle(), 
            newCache.getViewCount(), newCache.getLikeCount());
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
     * Normalizza i valori: lowercase, rimozione spazi multipli per confronto case-insensitive
     */
    private String generateQueryKey(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    // Normalizza il valore: lowercase e rimuovi spazi multipli
                    if (value != null) {
                        value = value.toLowerCase()
                                    .trim()
                                    .replaceAll("\\s+", " "); // Sostituisce spazi multipli con uno singolo
                    }
                    
                    sb.append(key).append("=").append(value).append("&");
                });
        
        String normalizedParams = sb.toString();
        log.debug("Query normalizzata per cache: {}", normalizedParams);
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(normalizedParams.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return normalizedParams;
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
                
                log.info("=== METADATI VIDEO ESTRATTI ===");
                log.info("  Video ID: {}", video.getVideoId());
                log.info("  Title: '{}'", video.getTitle());
                log.info("  Channel: '{}' ({})", video.getChannelTitle(), video.getChannelId());
                log.info("  Published: {}", video.getPublishedAt());
                log.info("  Views: {}", video.getViewCount());
                log.info("  Likes: {}", video.getLikeCount());
                log.info("  Comments: {}", video.getCommentCount());
                log.info("  Duration: {}", video.getDuration());
                log.info("================================");
            }
        } catch (Exception e) {
            log.error("Errore nell'estrazione dei metadati del video: {}", e.getMessage());
        }
    }
    
    /**
     * Estrae i video dal JSON di una ricerca e li salva nel database
     */
    private void extractAndSaveVideosFromSearch(String searchResponseJson) {
        try {
            JsonNode root = objectMapper.readTree(searchResponseJson);
            JsonNode items = root.path("items");
            
            if (!items.isArray()) {
                return;
            }
            
            int savedCount = 0;
            for (JsonNode item : items) {
                // Estrai ID del video
                JsonNode idNode = item.path("id");
                String videoId = null;
                
                if (idNode.has("videoId")) {
                    videoId = idNode.path("videoId").asText();
                } else if (idNode.isTextual()) {
                    videoId = idNode.asText();
                }
                
                if (videoId == null || videoId.isEmpty()) {
                    continue;
                }
                
                // Verifica se il video esiste già nel database
                Optional<CachedVideo> existing = videoRepository.findByVideoId(videoId);
                CachedVideo video;
                
                if (existing.isPresent()) {
                    video = existing.get();
                    log.debug("Aggiornamento video esistente: {}", videoId);
                } else {
                    video = new CachedVideo();
                    video.setVideoId(videoId);
                    log.debug("Nuovo video trovato: {}", videoId);
                }
                
                // Estrai snippet (metadati base disponibili nella ricerca)
                JsonNode snippet = item.path("snippet");
                if (!snippet.isMissingNode()) {
                    video.setTitle(snippet.path("title").asText(null));
                    video.setDescription(snippet.path("description").asText(null));
                    video.setChannelId(snippet.path("channelId").asText(null));
                    video.setChannelTitle(snippet.path("channelTitle").asText(null));
                    video.setPublishedAt(snippet.path("publishedAt").asText(null));
                    
                    // Estrai thumbnail URL
                    JsonNode thumbnails = snippet.path("thumbnails");
                    if (thumbnails.has("high")) {
                        video.setThumbnailUrl(thumbnails.path("high").path("url").asText(null));
                    } else if (thumbnails.has("medium")) {
                        video.setThumbnailUrl(thumbnails.path("medium").path("url").asText(null));
                    } else if (thumbnails.has("default")) {
                        video.setThumbnailUrl(thumbnails.path("default").path("url").asText(null));
                    }
                }
                
                // Nota: Le statistiche (views, likes, ecc.) NON sono disponibili nella search API
                // Verranno popolate quando si chiama getVideoDetails() con part=statistics
                
                // Salva il video nel database
                videoRepository.save(video);
                log.info("Video salvato da search: videoId={}, title='{}', channel='{}'",
                    video.getVideoId(), video.getTitle(), video.getChannelTitle());
                savedCount++;
            }
            
            log.info("=== RIEPILOGO SALVATAGGIO VIDEO DALLA SEARCH ===");
            log.info("  Totale video estratti e salvati: {}", savedCount);
            log.info("================================================");
            
        } catch (Exception e) {
            log.error("Errore nell'estrazione dei video dalla ricerca: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Estrae e salva i metadati dalla risposta di ricerca di YouTube
     */
    private void extractAndSaveSearchMetadata(String responseJson, Map<String, String> params, CachedSearchResult cache) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            
            // Salva i parametri della query
            cache.setQuery(params.get("q"));
            cache.setOrderBy(params.get("order"));
            cache.setVideoType(params.get("type"));
            
            String maxResultsStr = params.get("maxResults");
            if (maxResultsStr != null) {
                cache.setMaxResults(Integer.parseInt(maxResultsStr));
            }
            
            // Estrai metadati dalla risposta
            cache.setNextPageToken(root.path("nextPageToken").asText(null));
            cache.setPrevPageToken(root.path("prevPageToken").asText(null));
            cache.setRegionCode(root.path("regionCode").asText(null));
            
            JsonNode pageInfo = root.path("pageInfo");
            if (!pageInfo.isMissingNode()) {
                cache.setTotalResults(pageInfo.path("totalResults").asInt(0));
            }
            
            log.info("=== METADATI SEARCH ESTRATTI ===");
            log.info("  Query text: '{}'", cache.getQuery());
            log.info("  Order by: {}", cache.getOrderBy());
            log.info("  Video type: {}", cache.getVideoType());
            log.info("  Max results: {}", cache.getMaxResults());
            log.info("  Total results: {}", cache.getTotalResults());
            log.info("  Region: {}", cache.getRegionCode());
            log.info("================================");
                
        } catch (Exception e) {
            log.error("Errore nell'estrazione dei metadati della ricerca: {}", e.getMessage());
        }
    }
    
    /**
     * Ricostruisce una risposta di ricerca dai video salvati nel database
     * ATTENZIONE: Questa è una risposta PARZIALE basata sui dati disponibili
     */
    private String reconstructSearchResponse(Map<String, String> params, CachedSearchResult expiredCache) {
        try {
            // Estrai parametri della query
            String query = params.get("q");
            if (query == null || query.trim().isEmpty()) {
                log.warn("Query parameter 'q' mancante, impossibile ricostruire risposta");
                return null;
            }
            
            int maxResults = 5; // Default YouTube
            try {
                String maxResultsStr = params.get("maxResults");
                if (maxResultsStr != null) {
                    maxResults = Integer.parseInt(maxResultsStr);
                }
            } catch (NumberFormatException e) {
                log.warn("maxResults non valido, uso default: 5");
            }
            
            // Cerca video nel DB che corrispondono alla query
            List<CachedVideo> matchingVideos = findVideosMatchingQuery(query, maxResults);
            
            if (matchingVideos.isEmpty()) {
                log.warn("Nessun video trovato nel database per la query: {}", query);
                return null;
            }
            
            // Costruisci un JSON compatibile con YouTube API
            ObjectNode response = objectMapper.createObjectNode();
            response.put("kind", "youtube#searchListResponse");
            response.put("etag", "reconstructed-from-db");
            
            // Usa metadati dalla cache scaduta se disponibili
            if (expiredCache != null && expiredCache.getRegionCode() != null) {
                response.put("regionCode", expiredCache.getRegionCode());
            }
            
            // PageInfo
            ObjectNode pageInfo = response.putObject("pageInfo");
            pageInfo.put("totalResults", matchingVideos.size());
            pageInfo.put("resultsPerPage", matchingVideos.size());
            
            // Items array
            ArrayNode items = response.putArray("items");
            for (CachedVideo video : matchingVideos) {
                ObjectNode item = items.addObject();
                item.put("kind", "youtube#searchResult");
                item.put("etag", "reconstructed");
                
                // ID
                ObjectNode id = item.putObject("id");
                id.put("kind", "youtube#video");
                id.put("videoId", video.getVideoId());
                
                // Snippet
                ObjectNode snippet = item.putObject("snippet");
                snippet.put("publishedAt", video.getPublishedAt() != null ? video.getPublishedAt() : "");
                snippet.put("channelId", video.getChannelId() != null ? video.getChannelId() : "");
                snippet.put("title", video.getTitle() != null ? video.getTitle() : "");
                snippet.put("description", video.getDescription() != null ? video.getDescription() : "");
                snippet.put("channelTitle", video.getChannelTitle() != null ? video.getChannelTitle() : "");
                
                // Thumbnails
                if (video.getThumbnailUrl() != null) {
                    ObjectNode thumbnails = snippet.putObject("thumbnails");
                    ObjectNode defaultThumb = thumbnails.putObject("default");
                    defaultThumb.put("url", video.getThumbnailUrl());
                    ObjectNode mediumThumb = thumbnails.putObject("medium");
                    mediumThumb.put("url", video.getThumbnailUrl());
                    ObjectNode highThumb = thumbnails.putObject("high");
                    highThumb.put("url", video.getThumbnailUrl());
                }
            }
            
            String result = objectMapper.writeValueAsString(response);
            log.info("Risposta ricostruita con {} video dal database (PARZIALE - no pagination)", matchingVideos.size());
            return result;
            
        } catch (Exception e) {
            log.error("Errore nella ricostruzione della risposta: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Trova video nel database che corrispondono alla query di ricerca
     * Confronto case-insensitive e normalizzato (senza spazi multipli)
     */
    private List<CachedVideo> findVideosMatchingQuery(String query, int maxResults) {
        // Normalizza la query: lowercase e spazi singoli
        String queryNormalized = query.toLowerCase().trim().replaceAll("\\s+", " ");
        
        log.debug("Ricerca video con query normalizzata: '{}'", queryNormalized);
        
        // Cerca video che contengono la query nel titolo, descrizione o channel
        return videoRepository.findAll().stream()
            .filter(v -> {
                if (v.getTitle() != null && normalizeForSearch(v.getTitle()).contains(queryNormalized)) {
                    return true;
                }
                if (v.getDescription() != null && normalizeForSearch(v.getDescription()).contains(queryNormalized)) {
                    return true;
                }
                if (v.getChannelTitle() != null && normalizeForSearch(v.getChannelTitle()).contains(queryNormalized)) {
                    return true;
                }
                return false;
            })
            // Ordina per data di pubblicazione (più recenti prima)
            .sorted((v1, v2) -> {
                String date1 = v1.getPublishedAt() != null ? v1.getPublishedAt() : "";
                String date2 = v2.getPublishedAt() != null ? v2.getPublishedAt() : "";
                return date2.compareTo(date1);
            })
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * Normalizza una stringa per la ricerca: lowercase e spazi singoli
     */
    private String normalizeForSearch(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Conta il numero di video in una risposta JSON
     */
    private int countVideosInResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode items = root.path("items");
            return items.isArray() ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Crea una risposta di ricerca vuota ma valida
     */
    private String createEmptySearchResponse() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("kind", "youtube#searchListResponse");
        response.put("etag", "no-data-available");
        
        ObjectNode pageInfo = response.putObject("pageInfo");
        pageInfo.put("totalResults", 0);
        pageInfo.put("resultsPerPage", 0);
        
        response.putArray("items");
        
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"kind\":\"youtube#searchListResponse\",\"items\":[]}";
        }
    }
    
    /**
     * Crea una risposta video vuota ma valida
     */
    private String createEmptyVideoResponse() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("kind", "youtube#videoListResponse");
        response.put("etag", "no-data-available");
        
        ObjectNode pageInfo = response.putObject("pageInfo");
        pageInfo.put("totalResults", 0);
        pageInfo.put("resultsPerPage", 0);
        
        response.putArray("items");
        
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"kind\":\"youtube#videoListResponse\",\"items\":[]}";
        }
    }
}
