# API Gateway

[![Build](https://github.com/rsanto0/auth-service/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/rsanto0/auth-service/actions/workflows/ci.yml)
[![Tests](https://github.com/rsanto0/auth-service/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/rsanto0/auth-service/actions/workflows/tests.yml)
[![codecov](https://codecov.io/gh/rsanto0/auth-service/branch/main/graph/badge.svg)](https://codecov.io/gh/rsanto0/auth-service)
[![GitHub release](https://img.shields.io/github/v/release/rsanto0/auth-service)](https://github.com/rsanto0/auth-service/releases)
[![License](https://img.shields.io/github/license/rsanto0/auth-service)](https://github.com/rsanto0/auth-service/blob/main/LICENSE)



Gateway de API desenvolvido com Spring Cloud Gateway para roteamento e autenticação JWT de microserviços.

## 🚀 Tecnologias

- **Java 17**
- **Spring Boot 3.3.2**
- **Spring Cloud Gateway 2023.0.3**
- **Spring WebFlux**
- **JWT (jsonwebtoken 0.12.3)**
- **Maven**

## 📋 Funcionalidades

- ✅ Roteamento de requisições para microserviços
- ✅ Autenticação JWT automática
- ✅ Injeção de headers de usuário
- ✅ Logs estruturados
- ✅ Configuração declarativa de rotas

## 🏗️ Arquitetura

```
src/main/java/com/exemplo/gateway/
├── filter/             # Filtros personalizados
│   └── JwtAuthFilter   # Validação JWT
├── config/             # Configurações
└── GatewayApplication  # Classe principal
```

## 🔧 Configuração

### Servidor
- **Porta**: 8080

### JWT
- **Algoritmo**: HMAC-SHA
- **Secret**: Configurável via `jwt.secret`

### Rotas Configuradas

#### Auth Service (Público)
- **Padrão**: `/auth/**`
- **Destino**: http://localhost:8081
- **Autenticação**: Não requerida

#### Sistema Ponto (Protegido)
- **Padrão**: `/api/**`
- **Destino**: http://localhost:8082
- **Autenticação**: JWT obrigatório
- **Filtros**: JwtAuthFilter + StripPrefix

## 🔐 Autenticação JWT

### Headers Injetados
Após validação do JWT, o gateway adiciona headers para os microserviços:

```
X-User-Id: <userId>
X-User-Login: <login>
X-User-Role: <role>
```

### Fluxo de Autenticação
1. Cliente envia requisição com `Authorization: Bearer <token>`
2. Gateway valida JWT usando secret compartilhado
3. Se válido, injeta headers de usuário
4. Encaminha para microserviço de destino
5. Se inválido, retorna 401 Unauthorized

## 📡 Roteamento

### Requisições Públicas
```bash
# Login (sem autenticação)
POST http://localhost:8080/auth/login

# Validação de token
POST http://localhost:8080/auth/validate

# Criação de usuários
POST http://localhost:8080/auth/users
```

### Requisições Protegidas
```bash
# Sistema de ponto (requer JWT)
POST http://localhost:8080/api/pontos/1/registrar
GET  http://localhost:8080/api/pontos/1
GET  http://localhost:8080/api/funcionarios

# Headers obrigatórios
Authorization: Bearer <jwt-token>
```

## 🏃♂️ Executando

### Pré-requisitos
- Java 17+
- Maven 3.6+
- Auth Service rodando na porta 8081
- Sistema Ponto rodando na porta 8082

### Comandos
```bash
# Compilar
mvn clean compile

# Executar
mvn spring-boot:run
```

Gateway disponível em: http://localhost:8080

## 🔄 Integração com Microserviços

### Auth Service (8081)
- Endpoints públicos para login e validação
- Geração de tokens JWT
- Gerenciamento de usuários

### Sistema Ponto (8082)
- Endpoints protegidos por JWT
- Recebe headers de usuário do gateway
- Controle de ponto e funcionários

## 📊 Logs

Logs DEBUG habilitados para `com.exemplo.gateway`:

- `[JWT]` - Validação de tokens
- `[ROUTE]` - Roteamento de requisições
- `[FILTER]` - Execução de filtros

## ⚙️ Configuração Avançada

### Personalizar Rotas
Edite `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: novo-servico
          uri: http://localhost:8083
          predicates:
            - Path=/novo/**
          filters:
            - name: JwtAuthFilter
```

### Configurar JWT Secret
```yaml
jwt:
  secret: sua-chave-secreta-aqui
```

## 🛡️ Segurança

### Pontos de Atenção
- Secret JWT deve ser o mesmo em todos os serviços
- CORS não configurado
- Rate limiting não implementado

### Melhorias Recomendadas
1. Configurar CORS para produção
2. Implementar rate limiting
3. Adicionar circuit breaker
4. Configurar SSL/TLS
5. Implementar cache de validação JWT

## 🔧 Troubleshooting

### Erro 401 - Unauthorized
- Verificar se token JWT está no header
- Validar formato: `Authorization: Bearer <token>`
- Confirmar se secret é o mesmo do auth-service

### Erro 503 - Service Unavailable
- Verificar se microserviços estão rodando
- Confirmar portas configuradas (8081, 8082)
- Checar conectividade de rede

### Logs de Debug
```bash
# Habilitar logs detalhados
logging.level.com.exemplo.gateway=DEBUG
```

## 📝 Exemplo de Uso

### 1. Obter Token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","senha":"admin123"}'
```

### 2. Usar Token
```bash
curl -X GET http://localhost:8080/api/funcionarios \
  -H "Authorization: Bearer <token-jwt>"
```

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT.