package com.exemplo.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para JwtAuthFilter
 * Valida comportamento de autenticação JWT via Auth Service
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;
    private JwtAuthFilter.Config config;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(webClient);
        config = new JwtAuthFilter.Config();
    }

    /**
     * Testa cenário onde token JWT não está presente no header Authorization
     * Deve retornar 401 Unauthorized sem chamar Auth Service
     */
    @Test
    void testSemTokenJWT_DeveRetornar401() {
        // Arrange - Criar request sem header Authorization
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        GatewayFilter filter = jwtAuthFilter.apply(config);

        // Act & Assert - Executar filtro e verificar resultado
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectComplete()
                .verify();

        // Verificar se status foi definido como 401
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        
        // Verificar que Auth Service não foi chamado
        verifyNoInteractions(webClient);
        
        // Verificar que chain não foi executado
        verifyNoInteractions(filterChain);
    }

    /**
     * Testa cenário onde header Authorization não começa com "Bearer "
     * Deve retornar 401 Unauthorized sem chamar Auth Service
     */
    @Test
    void testTokenSemBearerPrefix_DeveRetornar401() {
        // Arrange - Criar request com token inválido (sem Bearer)
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "InvalidToken123")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        GatewayFilter filter = jwtAuthFilter.apply(config);

        // Act & Assert - Executar filtro e verificar resultado
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectComplete()
                .verify();

        // Verificar se status foi definido como 401
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        
        // Verificar que Auth Service não foi chamado
        verifyNoInteractions(webClient);
    }

    /**
     * Testa cenário onde Auth Service valida token com sucesso
     * Deve injetar headers X-User-* e continuar cadeia de filtros
     */
    @Test
    void testTokenValido_DeveInjetarHeadersEContinuar() {
        // Arrange - Configurar mocks para resposta bem-sucedida do Auth Service
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("token-valido"));
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Criar request com token válido
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        GatewayFilter filter = jwtAuthFilter.apply(config);

        // Act & Assert - Executar filtro e verificar resultado
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectComplete()
                .verify();

        // Verificar que Auth Service foi chamado
        verify(webClient).post();
        verify(requestBodyUriSpec).uri(contains("/auth/validate"));
        verify(requestBodySpec).header("Authorization", "Bearer valid-jwt-token");
        
        // Verificar que chain foi executado (token válido)
        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    /**
     * Testa cenário onde Auth Service retorna erro (token inválido)
     * Deve retornar 401 Unauthorized sem continuar cadeia
     */
    @Test
    void testTokenInvalido_DeveRetornar401() {
        // Arrange - Configurar mocks para erro do Auth Service
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Token inválido")));

        // Criar request com token inválido
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        GatewayFilter filter = jwtAuthFilter.apply(config);

        // Act & Assert - Executar filtro e verificar resultado
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectComplete()
                .verify();

        // Verificar que Auth Service foi chamado
        verify(webClient).post();
        
        // Verificar se status foi definido como 401
        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        
        // Verificar que chain NÃO foi executado (token inválido)
        verifyNoInteractions(filterChain);
    }

    /**
     * Testa se headers X-User-* são injetados corretamente quando token é válido
     * Verifica se os valores fixos são adicionados ao request
     */
    @Test
    void testInjecaoDeHeaders_DeveAdicionarXUserHeaders() {
        // Arrange - Configurar mocks para resposta bem-sucedida
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("success"));
        
        // Capturar o exchange modificado
        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            
            // Verificar se headers foram injetados
            assert modifiedExchange.getRequest().getHeaders().getFirst("X-User-Id").equals("1");
            assert modifiedExchange.getRequest().getHeaders().getFirst("X-User-Login").equals("user");
            assert modifiedExchange.getRequest().getHeaders().getFirst("X-User-Role").equals("USER");
            
            return Mono.empty();
        });

        // Criar request com token válido
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        GatewayFilter filter = jwtAuthFilter.apply(config);

        // Act & Assert - Executar filtro
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectComplete()
                .verify();

        // Verificar que chain foi executado com headers injetados
        verify(filterChain).filter(any(ServerWebExchange.class));
    }
}