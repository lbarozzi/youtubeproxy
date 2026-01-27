package efohum.com.youtubeproxy.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import efohum.com.youtubeproxy.entity.ApiKey;
import efohum.com.youtubeproxy.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {
    
    private final ApiKeyRepository apiKeyRepository;
    
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateApiKey(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer daysValid) {
        
        ApiKey apiKey = new ApiKey();
        apiKey.setDescription(description);
        
        if (daysValid != null && daysValid > 0) {
            apiKey.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        }
        
        ApiKey savedKey = apiKeyRepository.save(apiKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", savedKey.getKeyValue());
        response.put("description", savedKey.getDescription());
        response.put("createdAt", savedKey.getCreatedAt());
        response.put("expiresAt", savedKey.getExpiresAt());
        response.put("isActive", savedKey.getIsActive());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        return ResponseEntity.ok(apiKeyRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiKey> getApiKey(@PathVariable Long id) {
        Optional<ApiKey> apiKey = apiKeyRepository.findById(id);
        return apiKey.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateApiKey(@PathVariable Long id) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
        
        if (apiKeyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "API-KEY disattivata con successo");
        response.put("id", id);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateApiKey(@PathVariable Long id) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
        
        if (apiKeyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        apiKey.setIsActive(true);
        apiKeyRepository.save(apiKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "API-KEY attivata con successo");
        response.put("id", id);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteApiKey(@PathVariable Long id) {
        if (!apiKeyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        apiKeyRepository.deleteById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "API-KEY eliminata con successo");
        response.put("id", id);
        
        return ResponseEntity.ok(response);
    }
}
