# ADR 0002: Layout de pacotes e regras de dependência de cada serviço

- Status: Proposto
- Data: 2026-11-11

## Contexto

Hoje cada serviço organiza o código por camada técnica (`controller`, `service`, `repository`, `domain`, `dto`). Tudo é `public`, o domínio é a entidade JPA e controllers internos usam repositórios direto (o `ArquiteturaTest` de cliente, financeiro e processo registra essas exceções). Nada impede que a estrutura se desfaça.

## Decisão

**Um módulo Maven por serviço, com estes pacotes sob `app.lexo`:**

```
adapter/
  in/    web, web/internal, event, scheduler
  out/   persistence, e um pacote por sistema externo (cliente, usuario, email, lock, cache, event)
application/
  port/in/    portas de entrada, comandos e respostas
  port/out/   portas de saída
  service/    um serviço por caso de uso
domain/       modelo em Java puro
```

**Regras de dependência**, verificadas pelo `ArquiteturaTest` de cada serviço:

- `domain` não importa Spring, JPA, `application` nem `adapter`.
- `application` importa só `domain`, as próprias portas e as anotações de estereótipo e de transação do Spring. Nenhum tipo de web, de persistência ou de segurança, como `AuthUser`.
- `adapter.in` chama só `application.port.in`. `adapter.out` só implementa `application.port.out`.
- `adapter.in` e `adapter.out` não se importam entre si, e não há ciclo entre pacotes.
- `@Transactional` só em `application.service`.
- Classes de `adapter` e de `application.service` são package-private. Só portas, comandos e respostas são `public`.

**Casos de uso e modelos**

- Uma porta de entrada, um serviço e um comando por caso de uso. O comando é imutável e valida a si mesmo no construtor.
- O escritório (`EscritorioId`) e o usuário entram no comando como ids tipados, não como `AuthUser`.
- Regra de negócio mora no domínio. O serviço orquestra: carrega, pede ao domínio, grava.

**Persistência e mapeamento**

- A entidade JPA (`...JpaEntity`) fica no adaptador, mapeada para a mesma tabela de hoje, com um mapper de duas vias para o domínio.
- O filtro por escritório fica só no adaptador de persistência.
- Portas de saída estreitas, uma por necessidade do caso de uso.

**Montagem:** injeção pelo construtor, com a varredura de classpath do Spring (`@Component`, `@RestController`). Java Config por serviço só se os testes precisarem subir partes isoladas.

**Testes por camada**

- Domínio e caso de uso: unitários, sem Spring, com portas simuladas ou falsas.
- Adaptadores de persistência: integração com Postgres (Testcontainers).
- Caminhos principais de cada serviço: teste de integração pela API HTTP, os mesmos de hoje.

**Bibliotecas:** `lexo-common` fica como está até a limpeza final. A `BaseEntity` só sai quando o último serviço migrar.

## Consequências

- Mais arquivos por serviço (um por caso de uso, mais mappers e portas), em troca de dependência verificada e domínio testável sem Spring.
- Os atalhos permitidos (pular porta de entrada em CRUD sem regra, sem mapper) são tratados em ADR própria, com critério de saída.
- O `ArquiteturaTest` passa a ser o guardião: violação nova falha o build.

## Referências

- Hombergs, cap. 3 (pacotes), cap. 4 (casos de uso), cap. 5 e 6 (adaptadores), cap. 8 (mapeamento), cap. 9 (montagem) e cap. 10 (proteger as fronteiras).
