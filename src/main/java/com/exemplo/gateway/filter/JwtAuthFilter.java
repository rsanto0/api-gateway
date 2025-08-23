package com.exemplo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Construtor com injeção do WebClient
     */
    public JwtAuthFilter(WebClient webClient) {
        super(Config.class);
        this.webClient = webClient;
    }
    
    /**
     * Parse da resposta JSON do auth-service
     */
    private Map<String, Object> parseJsonResponse(String jsonResponse) throws Exception {
        return objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * Cria filtro JWT que delega validação para Auth Service e injeta headers
     * @param config configuração do filtro
     * @return GatewayFilter que processa autenticação via Auth Service
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            logger.info("[JWT] 🔍 Processando requisição: {} {}", method, path);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("[JWT] ❌ Token JWT não encontrado ou inválido para {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            logger.info("[JWT] 🔗 Validando token via Auth Service: {}", authServiceUrl);
            
            return webClient.post()
                    .uri(authServiceUrl + "/auth/validate")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        logger.info("[JWT] ✅ Token válido! Processando claims do usuário");
                        logger.debug("[JWT] 📄 Resposta do Auth Service: {}", response);
                        
                        try {
                            // Parse da resposta JSON do auth-service
                            Map<String, Object> claims = parseJsonResponse(response);
                            
                            // Injeta headers com dados reais do token
                            String userId = String.valueOf(claims.get("userId"));
                            String userLogin = String.valueOf(claims.get("sub"));
                            String userRole = String.valueOf(claims.get("role"));
                            
                            var mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Login", userLogin)
                                .header("X-User-Role", userRole)
                                .build();
                            
                            logger.info("[JWT] 📦 Headers injetados: X-User-Id={}, X-User-Login={}, X-User-Role={}", userId, userLogin, userRole);
                            logger.info("[JWT] ➡️ Encaminhando para microserviço: {}", path);
                            
                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        } catch (Exception e) {
                            logger.error("[JWT] Erro ao processar resposta: {}", e.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                    })
                    .onErrorResume(error -> {
                        logger.error("[JWT] ❌ Token inválido para {}: {}", path, error.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }
    

    
    public static class Config {
        private String authServiceUrl;
        private boolean skipValidation = false;
        private String[] excludePaths = {};
        private int timeoutSeconds = 5;
        private boolean injectUserHeaders = true;
        private String userIdHeaderName = "X-User-Id";
        private String userLoginHeaderName = "X-User-Login";
        private String userRoleHeaderName = "X-User-Role";
        
        // Getters e Setters
        public String getAuthServiceUrl() { return authServiceUrl; }
        public void setAuthServiceUrl(String authServiceUrl) { this.authServiceUrl = authServiceUrl; }
        
        public boolean isSkipValidation() { return skipValidation; }
        public void setSkipValidation(boolean skipValidation) { this.skipValidation = skipValidation; }
        
        public String[] getExcludePaths() { return excludePaths; }
        public void setExcludePaths(String[] excludePaths) { this.excludePaths = excludePaths; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public boolean isInjectUserHeaders() { return injectUserHeaders; }
        public void setInjectUserHeaders(boolean injectUserHeaders) { this.injectUserHeaders = injectUserHeaders; }
        
        public String getUserIdHeaderName() { return userIdHeaderName; }
        public void setUserIdHeaderName(String userIdHeaderName) { this.userIdHeaderName = userIdHeaderName; }
        
        public String getUserLoginHeaderName() { return userLoginHeaderName; }
        public void setUserLoginHeaderName(String userLoginHeaderName) { this.userLoginHeaderName = userLoginHeaderName; }
        
        public String getUserRoleHeaderName() { return userRoleHeaderName; }
        public void setUserRoleHeaderName(String userRoleHeaderName) { this.userRoleHeaderName = userRoleHeaderName; }
    }
}