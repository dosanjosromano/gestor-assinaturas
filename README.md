
Sistema de gestão de assinaturas para um serviço de streaming: cadastro de
usuários, criação/cobrança/renovação/cancelamento de assinaturas.

Este README é deliberadamente prático — como subir e o que testar. 

## Subir localmente

```bash
docker compose up -d
```

Sobe só a infraestrutura: `postgres`, `redis`, `kafka`. Rode a aplicação
você mesmo via `./mvnw spring-boot:run -Dspring-boot.run.profiles=api` (ou
`worker`) — útil para desenvolvimento com reload rápido.

Para subir a stack completa, incluindo a aplicação containerizada:

```bash
docker compose --profile app up -d --build
```

Isso adiciona dois serviços à mesma rede:

- `app-api` — expõe a API REST em `:8080`
- `app-worker` — roda o scheduler de renovação e os consumers Kafka, sem
  rota HTTP de negócio (o Swagger UI também não sobe nesse perfil, ver
  abaixo)

Os dois rodam a partir da mesma imagem, só trocando `SPRING_PROFILES_ACTIVE`
— artefato único.

## Documentação da API

Com o perfil `api` no ar: [`http://localhost:8080/GestorAssinaturas/swagger-ui.html`](http://localhost:8080/swagger-ui.html)

A documentação lista os status HTTP condicionais reais de cada endpoint —
por exemplo, `POST /assinaturas` pode responder 201, 202, 402, 404 ou 409
dependendo do desfecho da cobrança, não só o caminho feliz.

## Fluxos para testar

### Usuário e assinatura (ciclo básico)

```bash
# criar usuário
curl -X POST localhost:8080/GestorAssinaturas/usuarios -H "Content-Type: application/json" \
  -d '{"nome":"Fulano","email":"fulano@teste.com"}'

# criar assinatura (o gateway mock aprova ~80% das vezes, recusa ~15%, erro técnico ~5%)
curl -X POST localhost:8080/GestorAssinaturas/assinaturas -H "Content-Type: application/json" \
  -d '{"usuarioId":"<id-do-usuario>","plano":"BASICO"}'

# consultar a assinatura ativa do usuário (cache-aside via Redis)
curl localhost:8080/GestorAssinaturas/assinaturas/usuario/<id-do-usuario>

# cancelar
curl -X DELETE localhost:8080/GestorAssinaturas/assinaturas/<id-da-assinatura>/cancelamento \
  -H "X-Usuario-Id: <id-do-usuario>"
```

### Atualizar/excluir usuário

```bash
# atualizar só o nome (email é opcional também)
curl -X PATCH localhost:8080/GestorAssinaturas/usuarios/<id> -H "Content-Type: application/json" \
  -d '{"nome":"Novo Nome"}'

# excluir (soft delete, idempotente — chamar de novo continua retornando 204)
curl -X DELETE localhost:8080/GestorAssinaturas/usuarios/<id>
```

Usuário inativo não consegue mais criar assinatura nova (409) e uma
assinatura ativa que ele já tinha para de renovar automaticamente — mesmo
que ainda esteja `ATIVA` no banco.

### Renovação automática

Esse fluxo não é uma chamada HTTP isolada — é
`RenovacaoScheduler` (worker) → Kafka → `RenovacaoEventConsumer` → cobrança
→ retry técnico se o gateway falhar. Não dá para testar via Swagger.

A migration `V7__popular_assinaturas.sql` já semeia assinaturas prontas
para esse teste (algumas vencidas e elegíveis, uma já suspensa, uma ainda
não vencida — para confirmar que o filtro de elegibilidade funciona nos
dois sentidos).

```bash
SPRING_PROFILES_ACTIVE=worker \
APP_SCHEDULER_RENOVACAO_CRON="0 * * * * *" \
./mvnw spring-boot:run
```

(6 campos: segundo, minuto, hora, dia, mês, dia-da-semana — `0 * * * * *` é
"a cada minuto", não `*/60 * * * * *`, que nunca dispara)

Em até um minuto o scheduler encontra as assinaturas elegíveis, publica um
evento por assinatura no tópico `renovacao-solicitada`, e os logs do worker
mostram o ciclo completo:

```
[inicia] busca de assinaturas elegíveis para renovação - ...
[finaliza] eventos de renovação publicados - total=N
[recebido] evento de renovação - assinaturaId=...
```

Se o gateway mock devolver erro técnico, o mesmo evento reaparece minutos
depois via `renovacao-solicitada-retry-*`.

## Executando os testes

```bash
./mvnw test
```

Testes que usam Testcontainers (Postgres/Kafka embutidos) precisam de
Docker disponível no ambiente.
