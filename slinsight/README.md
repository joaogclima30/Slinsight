# SL Insight

**Ocorrências urbanas, acompanhamento transparente e dados para entender a cidade.**

O SL Insight é uma plataforma de registro e acompanhamento de problemas urbanos, como falhas na iluminação pública e situações que precisam de atendimento. O cidadão informa o problema e sua localização; a administração acompanha a ocorrência, atualiza seu status e mantém um histórico das ações.

A visão do produto reúne uma API, uma interface web responsiva, um mapa de ocorrências e um dashboard de indicadores.

> **Estado do projeto:** backend em desenvolvimento, com os principais fluxos implementados. Frontend, mapa, dashboard e publicação fazem parte da evolução planejada. A ampliação dos testes de autorização de consulta é a próxima atividade. Este repositório ainda não representa uma versão final de produção.

## Funcionalidades

### Implementadas no backend

- Cadastro de usuários com senha armazenada como hash PBKDF2.
- Autenticação por e-mail e senha com HTTP Basic e proteção CSRF ativa.
- Perfis de acesso `CIDADAO` e `ADMIN`; o cadastro público cria somente cidadãos.
- Cadastro, consulta, atualização e desativação de categorias.
- Cadastro e consulta de localizações com endereço e coordenadas opcionais.
- Criação de ocorrências com autor obtido da autenticação.
- Listagem paginada com filtros por usuário, categoria e status.
- Consulta de detalhes e histórico de status.
- Registro do responsável autenticado em cada mudança de status.
- Restrição das consultas de ocorrências ao autor ou a um administrador.
- Mudança de status e gerenciamento de categorias restritos ao administrador nas rotas HTTP.
- Histórico e atualização de status gravados na mesma transação.
- Bloqueio pessimista para serializar mudanças concorrentes na mesma ocorrência.
- Validação de dados e tratamento de erros com `ProblemDetail` nos handlers implementados.
- Esquema versionado com Flyway, sem geração automática de tabelas pelo Hibernate.

### Experiência completa planejada

- Interface web responsiva com cadastro, acesso e acompanhamento de ocorrências.
- Mapa para selecionar a localização e explorar ocorrências com filtros.
- Dashboard com distribuição por categoria e status, evolução temporal e tempo de resolução.
- Fluxos administrativos integrados à interface.
- Documentação da API e testes de segurança HTTP mais abrangentes.
- Configuração de produção, HTTPS e deploy.

## Tecnologias

| Área | Tecnologias utilizadas |
|---|---|
| Linguagem | Java 21 |
| Aplicação | Spring Boot 4.1.2 |
| API e validação | Spring MVC e Jakarta Validation |
| Segurança | Spring Security, HTTP Basic, CSRF e PBKDF2 |
| Persistência | Spring Data JPA e Hibernate |
| Banco | PostgreSQL 17 |
| Migrations | Flyway |
| Ambiente local | Docker Compose |
| Build | Maven Wrapper |
| Testes | JUnit 5, Spring Boot Test e MockMvc |
| Redução de código repetitivo | Lombok |

As tecnologias do frontend, do mapa e dos gráficos serão definidas na implementação dessas etapas.

## Arquitetura

O backend está organizado por funcionalidade, mantendo próximas as classes de cada domínio:

```text
src/main/java/io/github/joaogclima30/slinsight/
├── categoria/
├── common/dto/
├── config/
├── exception/
├── historico/
├── indicador/
├── localizacao/
├── ocorrencia/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── specification/
└── usuario/
```

- **Controller:** recebe requisições e define as respostas HTTP.
- **DTO:** define os dados de entrada e saída, sem expor entidades diretamente.
- **Service:** aplica regras, permissões de consulta e limites de transação.
- **Repository:** realiza consultas e operações de persistência.
- **Model:** representa entidades e estados do domínio.
- **Specification:** combina filtros opcionais da listagem.
- **Config e exception:** concentram a configuração e o tratamento de erros.

O pacote `indicador` está reservado para a implementação das métricas do dashboard.

## Regras de negócio

### Ciclo de uma ocorrência

```text
ABERTA → EM_ANALISE → EM_ANDAMENTO → RESOLVIDA
   │          │             │
   └──────────┴─────────────┴────────→ CANCELADA
```

- Toda ocorrência começa como `ABERTA`, com um registro inicial no histórico.
- Apenas as transições previstas no enum são permitidas.
- `RESOLVIDA` e `CANCELADA` são estados definitivos: não existe reabertura.
- Se o problema voltar, o cidadão deve registrar uma nova ocorrência.
- Uma transição rejeitada não deve alterar o status, a data nem criar histórico.
- O autor da ocorrência permanece o mesmo quando um administrador altera seu status.
- Categoria e usuário precisam estar ativos para o cadastro da ocorrência.
- A desativação de uma categoria preserva o registro e os relacionamentos existentes.

## Executar localmente

### Requisitos

- JDK 21.
- Docker Desktop iniciado com suporte a containers Linux.
- Porta 5432 disponível para o PostgreSQL e 8080 para a aplicação.

O Maven Wrapper acompanha o projeto; não é necessário instalar Maven separadamente.

### 1. Iniciar o banco

Na raiz que contém `pom.xml` e `compose.yaml`:

```powershell
docker compose up -d --wait
```

### 2. Iniciar a API

```powershell
.\mvnw.cmd spring-boot:run
```

Também é possível executar `SlinsightApplication` pelo IntelliJ com SDK 21.

Endereço local da API: `http://localhost:8080`.

O Flyway executa as migrations na inicialização. O Hibernate usa `ddl-auto: validate` para verificar os mapeamentos, sem modificar as tabelas.

### 3. Testar

Com o banco em execução:

```powershell
.\mvnw.cmd test
```

Os testes de integração usam o banco configurado. Utilize um banco de desenvolvimento/testes, nunca o de produção. Os testes transacionais fazem rollback; o teste de concorrência confirma transações separadas e remove os próprios registros ao final.

Há testes para persistência, status, histórico, concorrência, filtros, paginação, cadastro e tratamento de erros. A adaptação e ampliação dos testes de consulta às novas permissões está pendente; não se deve assumir que toda a suíte já foi validada após essas últimas mudanças.

## Configuração do PostgreSQL

Valores padrão exclusivos do ambiente local:

| Configuração | Valor |
|---|---|
| Host | `localhost` |
| Porta | `5432` |
| Banco | `slinsight` |
| Usuário | `slinsight` |
| Senha | `slinsight_dev` |

A porta do Compose é publicada somente em `127.0.0.1`. Os dados são persistidos em um volume Docker.

O Compose aceita `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` e `DB_PORT` no ambiente ou em um arquivo `.env`. A aplicação aceita `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` como variáveis de ambiente; o Spring não carrega automaticamente o `.env` do Compose.

Se mudar a porta ou o nome do banco no Compose, ajuste também `DB_URL`, por exemplo:

```text
jdbc:postgresql://localhost:5433/slinsight
```

As variáveis de inicialização do PostgreSQL só criam usuário e banco em um volume novo. Alterar a senha no Compose não troca a senha de um banco já inicializado.

Para parar sem remover os dados:

```powershell
docker compose stop
```

`docker compose down` preserva o volume. A opção `down -v` remove os volumes e seus dados; não deve ser usada para apenas reiniciar o ambiente.

## Banco e migrations

| Tabela | Responsabilidade |
|---|---|
| `usuarios` | Identidade, hash de senha, perfil e situação do usuário |
| `categorias` | Classificação das ocorrências |
| `localizacoes` | Endereço e coordenadas |
| `ocorrencias` | Descrição, autor, categoria, localização e status atual |
| `historico_status` | Mudanças de status, responsáveis e observações |
| `flyway_schema_history` | Controle das migrations executadas pelo Flyway |

Migrations em `src/main/resources/db/migration`:

- `V1__create_initial_schema.sql`: esquema inicial e restrições.
- `V2__add_perfil_to_usuarios.sql`: perfis de acesso.

Uma migration aplicada não deve ser editada. Evoluções do esquema entram em novos arquivos versionados.

## Autenticação e CSRF

No Postman, escolha **Authorization → Basic Auth** e informe o e-mail e a senha original da conta. O hash nunca deve ser usado como senha do cliente.

Para operações de escrita (`POST`, `PUT`, `PATCH` e `DELETE`):

1. Consulte `GET /csrf`, usando a mesma autenticação quando aplicável.
2. Copie o campo `token` da resposta.
3. Envie o token no header `X-CSRF-TOKEN`.
4. Preserve o cookie de sessão recebido e use o mesmo host nas requisições.

O cadastro público `POST /usuarios` não exige login, mas exige CSRF. As demais rotas de negócio exigem autenticação. Consultas `GET` não exigem token CSRF.

O ambiente local ainda usa HTTP Basic. O fluxo de autenticação da interface web e a configuração de produção serão concluídos nas próximas etapas; credenciais em produção devem trafegar por HTTPS.

Novas contas são sempre `CIDADAO`. Não há criação pública de administradores. No desenvolvimento, a promoção de uma conta é feita manualmente no banco, após conferir sua identidade. O corpo de cadastro não recebe perfil.

## Endpoints

Os caminhos abaixo refletem os controllers atuais, incluindo os sufixos `/criar` e `/listar`.

| Método | Rota | Operação |
|---|---|---|
| GET | `/csrf` | Obter token CSRF |
| POST | `/usuarios` | Cadastrar cidadão |
| GET | `/usuarios/{id}` | Consultar usuário; exige autenticação, revisão de acesso por titular pendente |
| POST | `/categorias/criar` | Criar categoria — administrador |
| GET | `/categorias/listar` | Listar categorias |
| GET | `/categorias/{id}` | Consultar categoria |
| PUT | `/categorias/{id}` | Atualizar categoria — administrador |
| DELETE | `/categorias/{id}` | Desativar categoria — administrador |
| POST | `/localizacoes` | Cadastrar localização |
| GET | `/localizacoes/{id}` | Consultar localização |
| POST | `/ocorrencias/criar` | Criar ocorrência do usuário autenticado |
| GET | `/ocorrencias/listar` | Listar e filtrar ocorrências permitidas |
| GET | `/ocorrencias/{id}` | Consultar ocorrência — autor ou administrador |
| PATCH | `/ocorrencias/{id}/status` | Alterar status — administrador |
| GET | `/ocorrencias/{ocorrenciaId}/historico` | Consultar histórico — autor ou administrador |

### Criar ocorrência

`POST /ocorrencias/criar`

```json
{
  "titulo": "Poste com lâmpada apagada",
  "descricao": "A iluminação da rua está apagada há três dias.",
  "categoriaId": 8,
  "localizacaoId": 7
}
```

Use IDs existentes e uma categoria ativa. O autor é identificado pela autenticação, sem `usuarioId` no JSON.

### Alterar status

`PATCH /ocorrencias/7/status`

```json
{
  "status": "EM_ANDAMENTO",
  "observacao": "Atendimento iniciado pela equipe responsável."
}
```

O exemplo pressupõe que a ocorrência está em `EM_ANALISE`. O responsável pelo histórico é o administrador autenticado.

### Filtrar e paginar

```http
GET /ocorrencias/listar?categoriaId=8&status=ABERTA&page=0&size=5
```

Filtros são opcionais e combinados com **E**. Um cidadão acessa suas próprias ocorrências; o administrador também pode filtrar por `usuarioId`. Páginas começam em zero.

Formato de resposta paginada:

```json
{
  "content": [],
  "pagina": 0,
  "tamanho": 5,
  "totalElementos": 0,
  "totalPaginas": 0,
  "primeira": true,
  "ultima": true
}
```

## Respostas de erro

| Código | Situação |
|---|---|
| `400` | Dados, parâmetros ou JSON inválidos |
| `401` | Autenticação ausente ou inválida |
| `403` | Acesso negado ou token CSRF ausente/inválido |
| `404` | Recurso não encontrado |
| `409` | Duplicidade, restrição de integridade ou transição de status proibida |

Os handlers implementados usam `ProblemDetail`. Os erros produzidos pelos filtros de segurança ainda podem ter o formato padrão do Spring; a padronização desses casos faz parte dos próximos ajustes. As respostas não devem expor senhas, hashes ou stack traces.

## Próximas entregas

- [ ] Adaptar e ampliar os testes de consulta para cidadão, autor e administrador.
- [ ] Testar as permissões HTTP e o comportamento de CSRF.
- [ ] Revisar o acesso a dados pessoais e às demais consultas por ID.
- [ ] Concluir os fluxos de autenticação e autorização destinados ao frontend.
- [ ] Implementar indicadores e consultas geográficas.
- [ ] Construir frontend, mapa e dashboard.
- [ ] Documentar a API, validar os fluxos completos e preparar o deploy.

## Autor

João Gabriel — projeto de estudo e desenvolvimento de uma plataforma de acompanhamento de ocorrências urbanas.
