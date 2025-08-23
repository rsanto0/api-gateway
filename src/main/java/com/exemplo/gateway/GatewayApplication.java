package com.exemplo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway - Ponto de entrada único para microserviços
 * 
 * RESPONSABILIDADES:
 * • Roteamento: Direciona requisições para microserviços corretos
 * • Autenticação: Valida tokens JWT antes de rotear
 * • Autorização: Controla acesso baseado em roles de usuário
 * • Headers: Injeta informações de usuário (X-User-Id, X-User-Login, X-User-Role)
 * • Centralização: Unifica acesso a múltiplos serviços em uma única porta
 * • Segurança: Aplica políticas de segurança de forma consistente
 * 
 * FLUXO:
 * Cliente → Gateway (8080) → Auth Service (8081) | Sistema Ponto (8082)
 */
@SpringBootApplication
public class GatewayApplication {
    /**
     * Método principal que inicializa o API Gateway
     * @param args argumentos da linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}