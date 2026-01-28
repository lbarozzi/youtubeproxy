package efohum.com.youtubeproxy.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import efohum.com.youtubeproxy.entity.CachedVideo;
import efohum.com.youtubeproxy.repository.CachedSearchResultRepository;
import efohum.com.youtubeproxy.repository.CachedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller per accedere alle statistiche dei video cachati
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {
    
    private final CachedVideoRepository videoRepository;
    private final CachedSearchResultRepository searchResultRepository;
    
    /**
     * GET /api/statistics/video/{videoId}
     * Ottiene le statistiche di un video specifico dal database
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<Map<String, Object>> getVideoStatistics(@PathVariable String videoId) {
        log.info("Richiesta statistiche per video: {}", videoId);
        
        return videoRepository.findByVideoId(videoId)
                .map(video -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("videoId", video.getVideoId());
                    stats.put("title", video.getTitle());
                    stats.put("channelTitle", video.getChannelTitle());
                    stats.put("publishedAt", video.getPublishedAt());
                    stats.put("duration", video.getDuration());
                    stats.put("thumbnailUrl", video.getThumbnailUrl());
                    
                    Map<String, Long> statistics = new HashMap<>();
                    statistics.put("viewCount", video.getViewCount());
                    statistics.put("likeCount", video.getLikeCount());
                    statistics.put("commentCount", video.getCommentCount());
                    statistics.put("favoriteCount", video.getFavoriteCount());
                    stats.put("statistics", statistics);
                    
                    stats.put("cached", true);
                    stats.put("cachedAt", video.getCreatedAt());
                    stats.put("expiresAt", video.getExpiresAt());
                    stats.put("expired", video.isExpired());
                    
                    return ResponseEntity.ok(stats);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/statistics/videos
     * Ottiene le statistiche di tutti i video cachati
     */
    @GetMapping("/videos")
    public ResponseEntity<List<Map<String, Object>>> getAllVideosStatistics() {
        log.info("Richiesta statistiche per tutti i video");
        
        List<CachedVideo> videos = videoRepository.findAll();
        List<Map<String, Object>> result = videos.stream()
                .map(video -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("videoId", video.getVideoId());
                    stats.put("title", video.getTitle());
                    stats.put("channelTitle", video.getChannelTitle());
                    stats.put("viewCount", video.getViewCount());
                    stats.put("likeCount", video.getLikeCount());
                    stats.put("commentCount", video.getCommentCount());
                    stats.put("thumbnailUrl", video.getThumbnailUrl());
                    stats.put("duration", video.getDuration());
                    stats.put("cachedAt", video.getCreatedAt());
                    stats.put("expired", video.isExpired());
                    return stats;
                })
                .toList();
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * GET /api/statistics/top-viewed
     * Ottiene i video più visti dalla cache
     */
    @GetMapping("/top-viewed")
    public ResponseEntity<List<Map<String, Object>>> getTopViewedVideos(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Richiesta top {} video più visti", limit);
        
        List<CachedVideo> videos = videoRepository.findAll().stream()
                .filter(video -> !video.isExpired() && video.getViewCount() != null)
                .sorted((v1, v2) -> Long.compare(v2.getViewCount(), v1.getViewCount()))
                .limit(limit)
                .toList();
        
        List<Map<String, Object>> result = videos.stream()
                .map(video -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("videoId", video.getVideoId());
                    stats.put("title", video.getTitle());
                    stats.put("channelTitle", video.getChannelTitle());
                    stats.put("viewCount", video.getViewCount());
                    stats.put("likeCount", video.getLikeCount());
                    stats.put("thumbnailUrl", video.getThumbnailUrl());
                    return stats;
                })
                .toList();
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * GET /api/statistics/summary
     * Ottiene un riepilogo generale delle statistiche
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        log.info("Richiesta riepilogo statistiche");
        
        List<CachedVideo> videos = videoRepository.findAll();
        long validVideos = videos.stream().filter(v -> !v.isExpired()).count();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCachedVideos", videos.size());
        summary.put("validCachedVideos", validVideos);
        summary.put("expiredCachedVideos", videos.size() - validVideos);
        
        long totalViews = videos.stream()
                .filter(v -> !v.isExpired() && v.getViewCount() != null)
                .mapToLong(CachedVideo::getViewCount)
                .sum();
        summary.put("totalViews", totalViews);
        
        long totalLikes = videos.stream()
                .filter(v -> !v.isExpired() && v.getLikeCount() != null)
                .mapToLong(CachedVideo::getLikeCount)
                .sum();
        summary.put("totalLikes", totalLikes);
        
        long totalComments = videos.stream()
                .filter(v -> !v.isExpired() && v.getCommentCount() != null)
                .mapToLong(CachedVideo::getCommentCount)
                .sum();
        summary.put("totalComments", totalComments);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * DELETE /api/statistics/cache/clear
     * Pulisce tutta la cache (search e video)
     * Utile dopo modifiche al sistema di caching
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.warn("Richiesta pulizia cache completa");
        
        long searchCount = searchResultRepository.count();
        long videoCount = videoRepository.count();
        
        searchResultRepository.deleteAll();
        videoRepository.deleteAll();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Cache pulita con successo");
        result.put("searchResultsDeleted", searchCount);
        result.put("videosDeleted", videoCount);
        
        log.info("Cache pulita: {} search results, {} videos eliminati", searchCount, videoCount);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * DELETE /api/statistics/cache/search
     * Pulisce solo la cache delle ricerche
     */
    @DeleteMapping("/cache/search")
    public ResponseEntity<Map<String, Object>> clearSearchCache() {
        log.warn("Richiesta pulizia cache search");
        
        long count = searchResultRepository.count();
        searchResultRepository.deleteAll();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Cache search pulita con successo");
        result.put("deleted", count);
        
        log.info("Cache search pulita: {} record eliminati", count);
        
        return ResponseEntity.ok(result);
    }
}
