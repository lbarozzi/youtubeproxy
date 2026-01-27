package efohum.com.youtubeproxy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import efohum.com.youtubeproxy.entity.ApiKey;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    Optional<ApiKey> findByKeyValue(String keyValue);
    
    Optional<ApiKey> findByKeyValueAndIsActiveTrue(String keyValue);
}
