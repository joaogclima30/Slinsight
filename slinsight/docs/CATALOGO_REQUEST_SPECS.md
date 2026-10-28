# Catálogo de request specs implementados

Frases em português; parâmetros e exemplos completos estão nos arquivos de teste correspondentes. Todas as classes abaixo usam HTTP real e PostgreSQL descartável.

## CadastroAutenticacaoRequestTests.java

**describe cadastro e autenticação por HTTP real**

- quando a senha tem {0} caracteres, então cadastra cidadão com hash privado
- quando a senha tem {0} caracteres, então retorna 400 sem cadastrar
- quando o campo {0} é inválido, então retorna 400 sem cadastrar
- it quando cadastra sem CSRF, então retorna 403 sem persistir
- it quando repete e-mail com outra caixa, então retorna 409 e mantém um cadastro
- it quando cadastra o mesmo e-mail simultaneamente, então cria um e retorna 409 para o outro
- it quando autentica e encerra a sessão, então permite me e depois revoga o acesso
- it quando a sessão autentica, então troca o identificador e invalida o CSRF anterior
- quando as credenciais são {0}, então não autentica
- it quando tenta logout sem CSRF, então retorna 403 e mantém a sessão
- it quando repete login e logout, então não duplica dados de negócio
- it quando usa HTTP Basic válido, então autentica; credencial inválida retorna 401
- quando login tem CSRF {0}, então retorna 403 e não autentica
- quando cadastro tem {0}, então retorna 400 sem persistir

## CategoriaRequestTests.java

**describe administração de categorias**

- it quando admin cria, atualiza sem trocar nome e desativa duas vezes, então preserva o registro
- quando cidadão tenta {0}, então retorna 403 sem alterar categoria
- quando nome tem {0} caracteres, então valida sem persistir entradas inválidas
- it quando repete nome com outra caixa, então retorna 409 sem duplicar
- it quando atualiza para nome de outra categoria, então retorna 409 preservando ambas
- it quando cria categoria duplicada simultaneamente, então uma cria e outra recebe 409
- it quando desativa categoria com ocorrência, então preserva os relacionamentos
- quando admin usa {0} em categoria inexistente, então retorna 404
- it quando admin escreve sem CSRF, então retorna 403 sem persistir

## LocalizacaoRequestTests.java

**describe criação e limites de localização**

- quando latitude={0} e longitude={1}, então aceita os limites
- it quando não informa coordenadas, então permite cadastrar endereço
- quando falta {0}, então retorna 400 sem persistir
- quando {0}={1}, então retorna 400 sem persistir
- quando {0} é inválido, então retorna 400 sem persistir
- it quando repete o endereço, então permite duas localizações distintas
- it quando cria sem CSRF, então retorna 403 sem persistir
- it quando anônimo tenta criar com CSRF válido, então exige autenticação e não persiste

## OcorrenciaConsultaRequestTests.java

**describe consultas com filtros e paginação**

- it quando combina usuário, categoria e status, então aplica os filtros com AND
- it quando filtros não encontram registros, então retorna 200 com página vazia
- it quando navega páginas com datas iguais, então mantém ordem estável sem repetir nem perder registros
- quando consulta com {0}, então retorna 400
- quando size={0}, então aceita dentro do limite de cem
- quando histórico recebe {0}, então retorna 400
- it quando autor consulta histórico, então retorna criação antes da mudança de status

## OcorrenciaCriacaoRequestTests.java

**describe criação atômica e idempotente de ocorrências**

- it quando cria tentando informar outro autor, então usa o autenticado e grava ABERTA com um histórico
- quando {0} não existe, então retorna 404 sem ocorrência nem histórico
- it quando categoria está inativa, então retorna 409 sem gravação parcial
- quando {0} está ausente, então retorna 400 sem gravar
- quando categoriaId={0}, então retorna 400 sem gravar
- quando localizacaoId={0}, então retorna 400 sem gravar
- quando {0} contém apenas espaços, então retorna 400 sem gravação parcial
- quando título tem {0} caracteres, então aplica o limite de 200
- it quando repete a criação com a mesma chave, então retorna a mesma ocorrência e um histórico
- it quando cria duas vezes simultaneamente com a mesma chave, então persiste apenas uma ocorrência
- it quando cria sem CSRF, então retorna 403 e não altera tabelas

## OcorrenciaStatusRequestTests.java

**describe transições de status com histórico atômico**

- it quando admin segue o fluxo até resolver, então preserva autor e registra cada responsável e transição
- quando cancela a partir de {0}, então registra CANCELADA
- quando tenta ABERTA para {0}, então retorna 409 sem alterar data ou histórico
- quando ocorrência {0} tenta ir para {1}, então retorna 409 e permanece encerrada
- it quando cidadão tenta mudar status mesmo sendo autor, então retorna 403 sem efeito
- it quando admin envia sem CSRF, então retorna 403 sem alterar histórico
- it quando observação tem 2000 caracteres e tenta forjar responsável, então aceita e usa o admin autenticado
- it quando admin muda status inexistente, então retorna 404 sem gravação
- quando corpo é {0}, então retorna 400 sem efeito
- it quando duas requisições mudam para o mesmo status simultaneamente, então uma vence e outra recebe 409
- it quando análise e cancelamento concorrem, então serializa as mudanças sem perder histórico

## PrivacidadeRequestTests.java

**describe isolamento entre cidadãos e privilégios administrativos**

- it quando o cidadão consulta seus dados e histórico, então permite sem expor senha
- quando outro cidadão acessa {0}, então retorna 403 sem revelar dados
- it quando consulta cadastro alheio inexistente, então retorna 403 sem permitir enumerar usuários
- it quando admin consulta cadastro, ocorrência, localização e histórico alheios, então permite
- it quando lista sem filtro, então cidadão recebe somente suas ocorrências
- it quando perde perfil ADMIN durante a sessão, então vira cidadão sem poder escrever categorias
- it quando admin perde perfil, então me e listagem refletem cidadão na mesma sessão
- it quando admin perde perfil, então não pode mudar status usando a sessão antiga
- it quando desativa uma conta já autenticada, então bloqueia a próxima consulta
- quando anônimo acessa {0}, então exige autenticação
- it quando repete consultas, então não altera registros nem histórico

