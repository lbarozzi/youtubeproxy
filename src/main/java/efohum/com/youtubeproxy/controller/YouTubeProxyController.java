package efohum.com.youtubeproxy.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import efohum.com.youtubeproxy.service.YouTubeProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Replacement diretto delle API di Google YouTube con cache automatica
 * Base path: /youtube/v3
 * 
 * Sostituisce:
 * - https://www.googleapis.com/youtube/v3/search
 * - https://www.googleapis.com/youtube/v3/videos
 */
@RestController
@RequestMapping("/youtube/v3")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class YouTubeProxyController {
    
    private final YouTubeProxyService youTubeProxyService;
    
    /**
     * Replacement diretto per: GET https://www.googleapis.com/youtube/v3/search
     * 
     * Accetta TUTTI i parametri delle API Google YouTube senza validazione.
     * La validazione viene delegata alle API ufficiali per mantenere compatibilità totale.
     * 
     * Esempio: /youtube/v3/search?part=snippet&q=spring+boot&maxResults=10&type=video
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> search(@RequestParam Map<String, String> params) {
        log.info("YouTube API proxy - search request: {}", params);
        
        // Rimuovi il parametro 'key' se presente (viene usato quello configurato nell'app)
        params.remove("key");
        
        try {
            String response = youTubeProxyService.searchVideos(params);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch(IllegalStateException ops){
            // Special Case: API Key non configurata e dati mancanti nel DB
            log.info("API Key non configurata o dati non trovati nel DB per la richiesta: {}", params);
            return ResponseEntity.status(204)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}");
        } catch (Exception e) {
            log.error("Errore nella chiamata search API: ", e);
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": {\"message\": \"" + e.getMessage() + "\"}}");
        }
    }
    
    /**
     * Replacement diretto per: GET https://www.googleapis.com/youtube/v3/videos
     * 
     * Accetta TUTTI i parametri delle API Google YouTube senza validazione.
     * La validazione viene delegata alle API ufficiali per mantenere compatibilità totale.
     * 
     * Esempio: /youtube/v3/videos?part=snippet,statistics&id=dQw4w9WgXcQ
     */
    @GetMapping(value = "/videos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> videos(@RequestParam Map<String, String> params) {
        log.info("YouTube API proxy - videos request: {}", params);
        
        String videoId = params.get("id");
        
        // Rimuovi 'id' e 'key' dalla mappa per passare solo gli altri parametri
        params.remove("id");
        params.remove("key");
        
        try {
            String response = youTubeProxyService.getVideoDetails(videoId, params);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            log.error("Errore nella chiamata videos API: ", e);
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": {\"message\": \"" + e.getMessage() + "\"}}");
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\": \"UP\", \"service\": \"YouTube Proxy API\"}");
    }
}
