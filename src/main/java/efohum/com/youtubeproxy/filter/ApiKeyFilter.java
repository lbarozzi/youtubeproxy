package efohum.com.youtubeproxy.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import efohum.com.youtubeproxy.entity.ApiKey;
import efohum.com.youtubeproxy.repository.ApiKeyRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyFilter implements Filter {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${api.security.enabled:false}")
    private boolean securityEnabled;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // Se la sicurezza è disabilitata, passa oltre senza verifiche
        if (!securityEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        
        // Escludi H2 console e endpoint di gestione API-KEY dalla validazione
        if (path.startsWith("/h2-console") || 
            path.equals("/api/keys/generate") ||
            path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Richiedi API-KEY solo per gli endpoint protetti
        if (path.startsWith("/youtube/") || path.startsWith("/api/statistics/")) {
            String apiKey = extractApiKey(httpRequest);
            
            if (apiKey == null || apiKey.isEmpty()) {
                sendUnauthorizedResponse(httpResponse, "API-KEY mancante. Usa header 'X-API-Key' o parametro 'api_key'");
                return;
            }
            
            Optional<ApiKey> apiKeyEntity = apiKeyRepository.findByKeyValueAndIsActiveTrue(apiKey);
            
            if (apiKeyEntity.isEmpty() || !apiKeyEntity.get().isValid()) {
                sendUnauthorizedResponse(httpResponse, "API-KEY non valida o scaduta");
                return;
            }
            
            // Aggiorna ultimo utilizzo
            ApiKey key = apiKeyEntity.get();
            key.updateLastUsed();
            apiKeyRepository.save(key);
            
            log.debug("API-KEY valida: {} per path: {}", apiKey.substring(0, 10) + "...", path);
        }
        
        chain.doFilter(request, response);
    }
    
    private String extractApiKey(HttpServletRequest request) {
        // Cerca prima nell'header X-API-Key
        String apiKey = request.getHeader("X-API-Key");
        
        // Se non trovato, cerca nel query parameter api_key
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = request.getParameter("api_key");
        }
        
        return apiKey;
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", message);
        error.put("status", "UNAUTHORIZED");
        errorResponse.put("error", error);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
