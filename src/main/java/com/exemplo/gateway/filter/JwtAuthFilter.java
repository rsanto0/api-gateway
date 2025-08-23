package com.exemplo.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    @Value("${jwt.secret}")
    private String secret;
    
    /**
     * Construtor padrão que registra a classe de configuração
     */
    public JwtAuthFilter() {
        super(Config.class);
    }
    
    /**
     * Cria filtro JWT que valida tokens e injeta headers de usuário
     * @param config configuração do filtro
     * @return GatewayFilter que processa autenticação JWT
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.debug("Token JWT não encontrado");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            try {
                String token = authHeader.substring(7);
                Claims claims = validateToken(token);
                
                // Adiciona headers com info do usuário
                exchange.getRequest().mutate()
                    .header("X-User-Id", claims.get("userId").toString())
                    .header("X-User-Login", claims.getSubject())
                    .header("X-User-Role", claims.get("role").toString())
                    .build();
                
                logger.debug("JWT válido para usuário: {}", claims.getSubject());
                return chain.filter(exchange);
                
            } catch (Exception e) {
                logger.debug("Token JWT inválido: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
    
    /**
     * Valida token JWT usando secret compartilhado
     * @param token JWT a ser validado
     * @return claims do token se válido
     * @throws JwtException se token inválido/expirado
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public static class Config {
        // Configurações do filtro se necessário
    }
}