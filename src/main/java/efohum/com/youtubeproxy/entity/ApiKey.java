package efohum.com.youtubeproxy.entity;

import java.time.LocalDateTime;
import java.util.UUID;

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
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 64)
    private String keyValue;
    
    @Column(length = 255)
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime lastUsedAt;
    
    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (keyValue == null) {
            keyValue = generateApiKey();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    public boolean isValid() {
        if (!isActive) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
    
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    private String generateApiKey() {
        return "ypx_" + UUID.randomUUID().toString().replace("-", "");
    }
}
