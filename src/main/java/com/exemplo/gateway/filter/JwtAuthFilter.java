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

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    private final WebClient webClient;
    
    /**
     * Construtor com injeção do WebClient
     */
    public JwtAuthFilter(WebClient webClient) {
        super(Config.class);
        this.webClient = webClient;
    }
    
    /**
     * Cria filtro JWT que delega validação para Auth Service e injeta headers
     * @param config configuração do filtro
     * @return GatewayFilter que processa autenticação via Auth Service
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.debug("[JWT] Token não encontrado");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            logger.debug("[JWT] Validando token via Auth Service");
            
            return webClient.post()
                    .uri(authServiceUrl + "/auth/validate")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        logger.debug("[JWT] Token válido - resposta do Auth Service recebida");
                        
                        
                        // Injeta headers baseado no token válido
                        var mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", "1") // Valor fixo por simplicidade
                            .header("X-User-Login", "user") // Valor fixo por simplicidade  
                            .header("X-User-Role", "USER") // Valor fixo por simplicidade
                            .build();
                        
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(error -> {
                        logger.debug("[JWT] Token inválido: {}", error.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }
    

    
    public static class Config {
        // Configurações do filtro se necessário
    }
}