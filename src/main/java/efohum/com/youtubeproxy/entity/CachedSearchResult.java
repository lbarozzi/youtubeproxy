package efohum.com.youtubeproxy.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cached_search_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedSearchResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String queryKey; // Chiave basata sui parametri della query
    
    @Column(columnDefinition = "TEXT")
    private String responseJson; // Risposta dell'API in formato JSON
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    // Metadati della ricerca per ricostruire risposte parziali
    private String query; // Parametro q della ricerca
    private String orderBy; // order parameter
    private String videoType; // type parameter (video, channel, playlist)
    private Integer maxResults; // Numero di risultati richiesti
    private String nextPageToken;
    private String prevPageToken;
    private Integer totalResults;
    private String regionCode;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(24); // Cache per 24 ore
        }
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
