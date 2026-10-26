# SL Insight

## Banco local (PostgreSQL + Flyway)

Requisitos: JDK 21 e Docker Desktop iniciado com Linux containers.

1. Na raiz deste projeto, execute `docker compose up -d --wait`.
2. Execute `.\mvnw.cmd spring-boot:run` ou inicie SlinsightApplication no IntelliJ.
3. O Spring executa o Flyway antes de inicializar o JPA. A V1 cria as cinco tabelas e o Flyway registra a execucao em flyway_schema_history.
4. Com o banco iniciado, execute `.\mvnw.cmd test` para verificar o carregamento do contexto e das migrations.

Conexao local de desenvolvimento: localhost:5432, banco slinsight, usuario slinsight, senha slinsight_dev. A porta fica exposta somente no loopback.

O Compose aceita DB_NAME, DB_USERNAME, DB_PASSWORD e DB_PORT via ambiente ou arquivo .env. O Spring aceita DB_URL, DB_USERNAME e DB_PASSWORD como variaveis de ambiente da aplicacao (Run Configuration do IntelliJ). O Spring nao carrega automaticamente o .env do Compose. Se mudar banco ou porta, atualize tambem DB_URL, por exemplo jdbc:postgresql://localhost:5433/slinsight.

`docker compose stop` para o banco sem remover dados. `docker compose down` remove os containers e preserva o volume. Nao use `down -v` se quiser preservar os dados. As variaveis POSTGRES_* inicializam apenas volumes novos; alterar a senha no ambiente nao altera a senha de um banco existente.

## Esquema inicial

- usuarios: nome, email unico sem diferenciar maiusculas, senha_hash e indicador de ativo. Nunca armazenar senha em texto puro.
- categorias: nome unico e descricao.
- localizacoes: endereco brasileiro e coordenadas opcionais, sempre informadas em par.
- ocorrencias: autor, categoria e localizacao obrigatorios, titulo, descricao, status e datas.
- historico_status: ocorrencia, usuario responsavel pela alteracao, status anterior/novo, observacao e data.

Todos os IDs sao BIGINT com identidade. Status previstos: ABERTA, EM_ANALISE, EM_ANDAMENTO, RESOLVIDA e CANCELADA. Essas escolhas sao o ponto de partida para as futuras entidades Java.

A camada service devera gravar a mudanca de status e seu historico na mesma transacao, inclusive o registro inicial (status_anterior nulo), e atualizar atualizado_em. A migration cria a estrutura; ela nao implementa essas operacoes automaticamente. Exclusoes de registros referenciados sao bloqueadas para preservar o historico.

## Versionamento

O Flyway e o unico responsavel por alterar o esquema. Hibernate usa ddl-auto=validate e a inicializacao SQL paralela esta desativada. Nao alterar a V1 depois de aplicada: adicionar V2__descricao.sql, V3__descricao.sql etc. em src/main/resources/db/migration.
