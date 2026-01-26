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
@Table(name = "cached_videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedVideo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String videoId; // ID del video YouTube
    
    @Column(columnDefinition = "TEXT")
    private String responseJson; // Risposta dell'API in formato JSON
    
    // Metadati del video
    private String title;
    private String description;
    private String channelId;
    private String channelTitle;
    private String thumbnailUrl;
    
    // Statistiche del video
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long favoriteCount;
    
    // Dettagli del contenuto
    private String duration; // formato ISO 8601 (es: PT15M33S)
    private String publishedAt;
    private String category;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
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
