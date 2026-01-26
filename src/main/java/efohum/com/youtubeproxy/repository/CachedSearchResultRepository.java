package efohum.com.youtubeproxy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import efohum.com.youtubeproxy.entity.CachedSearchResult;

@Repository
public interface CachedSearchResultRepository extends JpaRepository<CachedSearchResult, Long> {
    
    Optional<CachedSearchResult> findByQueryKey(String queryKey);
}
