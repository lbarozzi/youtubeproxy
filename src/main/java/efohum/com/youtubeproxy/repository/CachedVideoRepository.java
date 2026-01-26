package efohum.com.youtubeproxy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import efohum.com.youtubeproxy.entity.CachedVideo;

@Repository
public interface CachedVideoRepository extends JpaRepository<CachedVideo, Long> {
    
    Optional<CachedVideo> findByVideoId(String videoId);
}
