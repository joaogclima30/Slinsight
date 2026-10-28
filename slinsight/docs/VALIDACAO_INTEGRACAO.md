# Validação de integração — SL Insight

Execução em 28/10/2026: **194 testes, 180 aprovados, 14 falhas, 0 erros e 0 ignorados**.

O Maven termina com BUILD FAILURE porque os testes registram comportamentos que ainda não atendem às regras aprovadas. Nenhum teste anterior foi apagado, ignorado ou afrouxado. Nenhuma regra da API foi corrigida nesta etapa.

## Como executar

Com Docker Desktop em execução, no terminal da pasta do projeto:

```powershell
.\mvnw.cmd verify
```

O comando roda também os testes anteriores. Não é necessário subir o Compose de desenvolvimento. O primeiro uso baixa as dependências e a imagem PostgreSQL.

## Ambiente e organização

- JUnit 5, Spring Boot e Maven existentes foram preservados; Testcontainers JDBC/PostgreSQL foram adicionados apenas com escopo test no pom.xml.
- Os novos testes estão em `src/test/java/io/github/joaogclima30/slinsight/requestspec` e compartilham `suporte/RequestSpecSupport.java`.
- Cada novo cenário abre HTTP real para o servidor em porta aleatória, autentica pela rota `/auth/login`, mantém cookies próprios e obtém CSRF pela rota `/csrf`. Há cobertura específica de HTTP Basic.
- Fixtures administrativas são inseridas diretamente no banco para preparar o cenário; as ações sob teste passam pela API. Não há mocks de banco, autenticação ou regras nos novos testes.
- Um customizador registrado em `src/test/resources/META-INF/spring.factories` direciona todos os contextos Spring de teste ao container, inclusive os testes antigos. A URL e credenciais de desenvolvimento/produção são sobrescritas no contexto de teste.
- O PostgreSQL 17 usa banco `slinsight_test`, credenciais exclusivas e porta aleatória; Flyway executa as migrations reais e Hibernate valida o esquema. O container é descartado pelo Testcontainers ao encerrar a JVM.
- Antes de cada novo cenário, a limpeza exige que `current_database()` seja exatamente `slinsight_test`; só então limpa as tabelas do container. As chamadas HTTP têm transações e commits reais.
- Os testes concorrentes usam duas sessões e uma barreira para liberar duas requisições ao mesmo tempo; verificam respostas, quantidade de registros e encadeamento do histórico.
- Testes antigos de unidade/serviço/MockMvc foram preservados e continuam complementando os request specs.

## Regras confirmadas pelo usuário

1. Repetir a mesma criação deve produzir somente uma ocorrência. Os testes identificam a mesma operação pelo cabeçalho `Idempotency-Key`, preservado entre tentativas sequenciais ou simultâneas, e esperam o mesmo ID e um único histórico inicial. O mecanismo ainda não existe na API.
2. Localização detalhada e dados pessoais do autor são privados para o autor e administradores. Administrador pode consultar nome/e-mail pelo cadastro do autor; cidadão alheio não pode consultar esses dados. Cadastrar novamente o mesmo endereço é permitido.
3. Quando ADMIN vira CIDADAO no banco, a sessão existente deve perder os privilégios imediatamente, conservando os direitos normais de cidadão.
4. Página negativa, tamanho zero/negativo e tamanho acima de 100 retornam 400; tamanho 100 é válido. Isso também se aplica à paginação do histórico.
5. A futura consulta de ocorrências na área será acessível somente a usuários cadastrados e não exporá dados pessoais do autor. Não existe atualmente uma rota específica para o mapa; esse fluxo futuro não foi inventado ou marcado como validado.

## Bugs reproduzidos

| Prioridade | Problema | Esperado | Observado |
|---|---|---|---|
| Alta | Sessão mantém autoridade ADMIN após rebaixamento | 403 em criação de categoria e alteração de status | Criação retorna 201 e mudança de status retorna 200 |
| Alta | Localização privada acessível por outro cidadão | GET de localização alheia retorna 403 | Retorna 200 com endereço e coordenadas |
| Média | Criação sem idempotência | Duas tentativas da mesma operação retornam o mesmo ID e persistem uma ocorrência | Retornam IDs diferentes e persistem duas ocorrências, inclusive simultaneamente |
| Média | Paginação aceita valores inválidos | 400 para page=-1, size=0/-1/101 | 200, tanto em ocorrências quanto no histórico |
| Média | Enumeração de cadastros por resposta diferente | Cidadão recebe 403 ao consultar cadastro alheio, existente ou não | Cadastro inexistente retorna 404, distinguindo-o do existente |

As falhas estão mantidas para orientar as futuras correções de produção; os casos e as evidências seguem abaixo. Concorrência de cadastro/categoria e de status é exercitada na execução; o resultado de cada caso está nos relatórios XML.

## Resultado por classe

| Classe | Executados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| SlinsightApplicationTests | 1 | 0 | 0 | 0 |
| PaginaResponseTests | 2 | 0 | 0 | 0 |
| ApiExceptionHandlerTests | 3 | 0 | 0 | 0 |
| OcorrenciaConcorrenciaIntegrationTests | 1 | 0 | 0 | 0 |
| OcorrenciaConsultaIntegrationTests | 17 | 0 | 0 | 0 |
| OcorrenciaStatusIntegrationTests | 5 | 0 | 0 | 0 |
| StatusOcorrenciaTests | 2 | 0 | 0 | 0 |
| EntidadesPersistenceTests | 1 | 0 | 0 | 0 |
| CadastroAutenticacaoRequestTests | 24 | 0 | 0 | 0 |
| CategoriaRequestTests | 16 | 0 | 0 | 0 |
| LocalizacaoRequestTests | 19 | 0 | 0 | 0 |
| OcorrenciaConsultaRequestTests | 18 | 8 | 0 | 0 |
| OcorrenciaCriacaoRequestTests | 20 | 2 | 0 | 0 |
| OcorrenciaStatusRequestTests | 27 | 0 | 0 | 0 |
| PrivacidadeRequestTests | 18 | 4 | 0 | 0 |
| AutenticacaoBasicHttpTests | 3 | 0 | 0 | 0 |
| AutenticacaoSessaoHttpTests | 5 | 0 | 0 | 0 |
| AutorizacaoOcorrenciaHttpTests | 4 | 0 | 0 | 0 |
| AutorizacaoUsuarioHttpTests | 7 | 0 | 0 | 0 |
| UsuarioCadastroIntegrationTests | 1 | 0 | 0 | 0 |

## Casos que falharam

### OcorrenciaConsultaRequestTests.paginacaoHistoricoInvalida(String)[1]

```text
[GET http://localhost:49511/ocorrencias/1/historico?page=-1
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.paginacaoHistoricoInvalida(String)[2]

```text
[GET http://localhost:49511/ocorrencias/1/historico?size=0
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.paginacaoHistoricoInvalida(String)[3]

```text
[GET http://localhost:49511/ocorrencias/1/historico?size=-1
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.paginacaoHistoricoInvalida(String)[4]

```text
[GET http://localhost:49511/ocorrencias/1/historico?size=101
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.parametrosInvalidos(String)[1]

```text
[GET http://localhost:49511/ocorrencias/listar?page=-1
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.parametrosInvalidos(String)[2]

```text
[GET http://localhost:49511/ocorrencias/listar?size=0
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.parametrosInvalidos(String)[3]

```text
[GET http://localhost:49511/ocorrencias/listar?size=-1
expected: 400
 but was: 200
```

### OcorrenciaConsultaRequestTests.parametrosInvalidos(String)[4]

```text
[GET http://localhost:49511/ocorrencias/listar?size=101
expected: 400
 but was: 200
```

### OcorrenciaCriacaoRequestTests.idempotenciaSimultanea

```text
expected: 2L
 but was: 1L
```

### OcorrenciaCriacaoRequestTests.idempotenciaSequencial

```text
expected: 1L
 but was: 2L
```

### PrivacidadeRequestTests.rebaixamentoImediato

```text
[POST http://localhost:49511/categorias/criar
expected: 403
 but was: 201
```

### PrivacidadeRequestTests.negarTerceiro(String)[4]

```text
[GET http://localhost:49511/localizacoes/1
expected: 403
 but was: 200
```

### PrivacidadeRequestTests.estrangeiroInexistente

```text
[GET http://localhost:49511/usuarios/99999999
expected: 403
 but was: 404
```

### PrivacidadeRequestTests.rebaixamentoStatus

```text
[PATCH http://localhost:49511/ocorrencias/1/status
expected: 403
 but was: 200
```

## Evidências e limites

Log completo: `target/validacao/execucao-completa.log`. Resultados individuais: `target/surefire-reports/TEST-*.xml` e arquivos `.txt`.

O catálogo em `docs/CATALOGO_REQUEST_SPECS.md` lista as frases de comportamento implementadas. Os cenários parametrizados têm seus valores declarados junto ao teste.

Checkout, pagamento, webhook, estoque, planos e integração de e-mail não existem neste projeto; não foram criados testes fictícios. Nenhum E2E de navegador foi adicionado. O mapa, frontend e dashboard ainda exigem seus próprios fluxos e validações quando implementados.
