package efohum.com.youtubeproxy.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import efohum.com.youtubeproxy.filter.ApiKeyFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {
    
    private final ApiKeyFilter apiKeyFilter;
    
    @Bean
    @ConditionalOnProperty(
        name = "api.security.enabled",
        havingValue = "true",
        matchIfMissing = false  // Disabilitato se la property non esiste
    )
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration() {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(apiKeyFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("apiKeyFilter");
        return registration;
    }
}
