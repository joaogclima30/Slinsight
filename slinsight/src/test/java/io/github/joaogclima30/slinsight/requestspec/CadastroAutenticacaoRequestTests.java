package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe cadastro e autenticação por HTTP real")
class CadastroAutenticacaoRequestTests extends RequestSpecSupport {
    private Map<String,Object> cadastro(String senha) {
        return Map.of("nome", "Maria", "email", "maria@example.com", "senha", senha, "perfil", "ADMIN");
    }

    @ParameterizedTest(name = "quando a senha tem {0} caracteres, então cadastra cidadão com hash privado")
    @ValueSource(ints = {12, 64})
    void cadastroValido(int tamanho) {
        var c = anonimo(); c.csrf();
        var r = esperar(c.enviar("POST", "/usuarios", cadastro("a".repeat(tamanho))), 201);
        assertThat(r.body()).doesNotContain("senha", "senhaHash", "senha_hash");
        var registro = db.queryForMap("select perfil,senha_hash from usuarios where id=?", id(r));
        assertThat(registro.get("perfil")).isEqualTo("CIDADAO");
        assertThat(encoder.matches("a".repeat(tamanho), (String) registro.get("senha_hash"))).isTrue();
        assertThat(contar("usuarios")).isEqualTo(1);
    }

    @ParameterizedTest(name = "quando a senha tem {0} caracteres, então retorna 400 sem cadastrar")
    @ValueSource(ints = {0, 11, 65})
    void senhaInvalida(int tamanho) {
        var c = anonimo(); c.csrf();
        esperar(c.enviar("POST", "/usuarios", cadastro("a".repeat(tamanho))), 400);
        assertThat(contar("usuarios")).isZero();
    }

    @ParameterizedTest(name = "quando o campo {0} é inválido, então retorna 400 sem cadastrar")
    @ValueSource(strings = {"nome", "email"})
    void campoInvalido(String campo) {
        var corpo = new java.util.HashMap<>(cadastro(SENHA)); corpo.put(campo, " ");
        var c = anonimo(); c.csrf();
        esperar(c.enviar("POST", "/usuarios", corpo), 400);
        assertThat(contar("usuarios")).isZero();
    }

    @Test @DisplayName("it quando cadastra sem CSRF, então retorna 403 sem persistir")
    void cadastroSemCsrf() {
        esperar(anonimo().enviar("POST", "/usuarios", cadastro(SENHA)), 403);
        assertThat(contar("usuarios")).isZero();
    }

    @Test @DisplayName("it quando repete e-mail com outra caixa, então retorna 409 e mantém um cadastro")
    void duplicado() {
        var c = anonimo(); c.csrf();
        esperar(c.enviar("POST", "/usuarios", cadastro(SENHA)), 201);
        var corpo = new java.util.HashMap<>(cadastro(SENHA)); corpo.put("email", "MARIA@example.com");
        esperar(c.enviar("POST", "/usuarios", corpo), 409);
        assertThat(contar("usuarios")).isEqualTo(1);
    }

    @Test @DisplayName("it quando cadastra o mesmo e-mail simultaneamente, então cria um e retorna 409 para o outro")
    void cadastroConcorrente() throws Exception {
        var a = anonimo(); a.csrf(); var b = anonimo(); b.csrf();
        var respostas = simultaneas(() -> a.enviar("POST", "/usuarios", cadastro(SENHA)),
                () -> b.enviar("POST", "/usuarios", cadastro(SENHA)));
        assertThat(respostas).extracting(r -> r.statusCode()).containsExactlyInAnyOrder(201, 409);
        assertThat(contar("usuarios")).isEqualTo(1);
    }

    @Test @DisplayName("it quando autentica e encerra a sessão, então permite me e depois revoga o acesso")
    void loginLogout() {
        long id = usuario("maria@example.com", "CIDADAO");
        var c = login("maria@example.com");
        var me = esperar(c.enviar("GET", "/usuarios/me", null), 200);
        assertThat(id(me)).isEqualTo(id);
        assertThat(me.body()).doesNotContain("senha");
        String cookieAnterior = c.cookies.getCookieStore().getCookies().stream()
                .filter(k -> k.getName().equals("JSESSIONID")).findFirst().orElseThrow().toString();
        esperar(c.enviar("POST", "/auth/logout", null), 204);
        esperar(c.enviar("GET", "/usuarios/me", null), 401);
        esperar(anonimo().enviar("GET", "/usuarios/me", null, false, Map.of("Cookie", cookieAnterior)), 401);
        assertThat(contar("usuarios")).isEqualTo(1);
    }

    @Test @DisplayName("it quando a sessão autentica, então troca o identificador e invalida o CSRF anterior")
    void rotacaoSessaoToken() {
        usuario("maria@example.com", "CIDADAO");
        var c = anonimo(); c.csrf();
        String tokenAnterior = c.token;
        String sessaoAnterior = c.cookies.getCookieStore().getCookies().stream()
                .filter(k -> k.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
        esperar(c.entrar("maria@example.com", SENHA), 204);
        assertThat(c.cookies.getCookieStore().getCookies().stream().filter(k -> k.getName().equals("JSESSIONID"))
                .findFirst().orElseThrow().getValue()).isNotEqualTo(sessaoAnterior);
        esperar(c.enviar("POST", "/localizacoes", endereco()), 403);
        c.csrf(); assertThat(c.token).isNotEqualTo(tokenAnterior);
        esperar(c.enviar("POST", "/localizacoes", endereco()), 201);
    }

    @ParameterizedTest(name = "quando as credenciais são {0}, então não autentica")
    @ValueSource(strings = {"erradas", "ausentes", "inativo"})
    void falhaLogin(String caso) {
        long id = usuario("maria@example.com", "CIDADAO");
        if (caso.equals("inativo")) db.update("update usuarios set ativo=false where id=?", id);
        var c = anonimo(); c.csrf();
        esperar(c.entrar(caso.equals("ausentes") ? "" : "maria@example.com",
                caso.equals("inativo") ? SENHA : "errada"), 401);
        esperar(c.enviar("GET", "/usuarios/me", null), 401);
    }

    @Test @DisplayName("it quando tenta logout sem CSRF, então retorna 403 e mantém a sessão")
    void logoutSemCsrf() {
        usuario("maria@example.com", "CIDADAO"); var c = login("maria@example.com");
        esperar(c.enviar("POST", "/auth/logout", null, false, Map.of()), 403);
        esperar(c.enviar("GET", "/usuarios/me", null), 200);
    }

    @Test @DisplayName("it quando repete login e logout, então não duplica dados de negócio")
    void repeticaoLogin() {
        usuario("maria@example.com", "CIDADAO");
        for (int i = 0; i < 2; i++) {
            var c = login("maria@example.com"); esperar(c.enviar("POST", "/auth/logout", null), 204);
        }
        assertThat(contar("usuarios")).isEqualTo(1);
        assertThat(contar("ocorrencias")).isZero();
    }

    @Test @DisplayName("it quando usa HTTP Basic válido, então autentica; credencial inválida retorna 401")
    void basic() {
        usuario("maria@example.com", "CIDADAO");
        var c = anonimo();
        String valido = java.util.Base64.getEncoder().encodeToString(("maria@example.com:" + SENHA).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        esperar(c.enviar("GET", "/usuarios/me", null, false, Map.of("Authorization", "Basic " + valido)), 200);
        esperar(anonimo().enviar("GET", "/usuarios/me", null, false, Map.of("Authorization", "Basic ZXJyYWRvOmVycmFkbw==")), 401);
    }

    @ParameterizedTest(name = "quando login tem CSRF {0}, então retorna 403 e não autentica")
    @ValueSource(strings = {"ausente", "inválido"})
    void loginCsrfInvalido(String caso) {
        usuario("maria@example.com", "CIDADAO"); var c = anonimo(); c.csrf();
        c.token = caso.equals("ausente") ? null : "token-invalido";
        esperar(c.entrar("maria@example.com", SENHA), 403);
        esperar(c.enviar("GET", "/usuarios/me", null), 401);
    }

    @ParameterizedTest(name = "quando cadastro tem {0}, então retorna 400 sem persistir")
    @ValueSource(strings = {"nome longo", "email longo", "email inválido", "nome só espaços"})
    void outrosCamposInvalidos(String caso) {
        var corpo = new java.util.HashMap<>(cadastro(SENHA));
        switch (caso) {
            case "nome longo" -> corpo.put("nome", "a".repeat(151));
            case "email longo" -> corpo.put("email", "a".repeat(240) + "@example.com.br");
            case "email inválido" -> corpo.put("email", "email-sem-arroba");
            default -> corpo.put("nome", "    ");
        }
        var c = anonimo(); c.csrf(); esperar(c.enviar("POST", "/usuarios", corpo), 400);
        assertThat(contar("usuarios")).isZero();
    }
}
