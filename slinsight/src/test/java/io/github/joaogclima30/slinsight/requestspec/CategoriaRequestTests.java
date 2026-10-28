package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe administração de categorias")
class CategoriaRequestTests extends RequestSpecSupport {
    Cliente admin, cidadao;
    @BeforeEach void preparar() {
        usuario("admin@example.com", "ADMIN"); usuario("cidadao@example.com", "CIDADAO");
        admin = login("admin@example.com"); cidadao = login("cidadao@example.com");
    }

    @Test @DisplayName("it quando admin cria, atualiza sem trocar nome e desativa duas vezes, então preserva o registro")
    void cicloAdministrativo() {
        long id = categoria(admin);
        esperar(admin.enviar("PUT", "/categorias/" + id, Map.of("nome", "Iluminação", "descricao", "Atualizada")), 200);
        esperar(admin.enviar("DELETE", "/categorias/" + id, null), 204);
        esperar(admin.enviar("DELETE", "/categorias/" + id, null), 204);
        assertThat(objeto(esperar(cidadao.enviar("GET", "/categorias/" + id, null), 200))).containsEntry("ativo", false);
        assertThat(contar("categorias")).isEqualTo(1);
    }

    @ParameterizedTest(name = "quando cidadão tenta {0}, então retorna 403 sem alterar categoria")
    @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
    void escritaRestrita(String metodo) {
        long id = categoria(admin); var antes = db.queryForList("select * from categorias");
        esperar(cidadao.enviar(metodo, metodo.equals("POST") ? "/categorias/criar" : "/categorias/" + id,
                metodo.equals("DELETE") ? null : Map.of("nome", "Alterada")), 403);
        assertThat(db.queryForList("select * from categorias")).isEqualTo(antes);
    }

    @ParameterizedTest(name = "quando nome tem {0} caracteres, então valida sem persistir entradas inválidas")
    @ValueSource(ints = {0, 100, 101})
    void tamanhoNome(int tamanho) {
        esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "a".repeat(tamanho))), tamanho == 100 ? 201 : 400);
        assertThat(contar("categorias")).isEqualTo(tamanho == 100 ? 1 : 0);
    }

    @Test @DisplayName("it quando repete nome com outra caixa, então retorna 409 sem duplicar")
    void nomeDuplicado() {
        categoria(admin);
        esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "ILUMINAÇÃO")), 409);
        assertThat(contar("categorias")).isEqualTo(1);
    }

    @Test @DisplayName("it quando atualiza para nome de outra categoria, então retorna 409 preservando ambas")
    void atualizacaoDuplicada() {
        long original = categoria(admin);
        long outra = id(esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "Buracos")), 201));
        var antes = db.queryForList("select * from categorias order by id");
        esperar(admin.enviar("PUT", "/categorias/" + outra, Map.of("nome", "ILUMINAÇÃO")), 409);
        assertThat(db.queryForList("select * from categorias order by id")).isEqualTo(antes);
        esperar(admin.enviar("GET", "/categorias/" + original, null), 200);
    }

    @Test @DisplayName("it quando cria categoria duplicada simultaneamente, então uma cria e outra recebe 409")
    void concorrencia() throws Exception {
        var outroAdmin = login("admin@example.com");
        var respostas = simultaneas(
                () -> admin.enviar("POST", "/categorias/criar", Map.of("nome", "Nova categoria")),
                () -> outroAdmin.enviar("POST", "/categorias/criar", Map.of("nome", "Nova categoria")));
        assertThat(respostas).extracting(r -> r.statusCode()).containsExactlyInAnyOrder(201, 409);
        assertThat(contar("categorias")).isEqualTo(1);
    }

    @Test @DisplayName("it quando desativa categoria com ocorrência, então preserva os relacionamentos")
    void desativacaoPreservaDados() {
        long cat = categoria(admin); long loc = localizacao(cidadao); long ocorrencia = ocorrencia(cidadao, cat, loc);
        esperar(admin.enviar("DELETE", "/categorias/" + cat, null), 204);
        esperar(cidadao.enviar("GET", "/ocorrencias/" + ocorrencia, null), 200);
        assertThat(contar("historico_status")).isEqualTo(1);
    }

    @ParameterizedTest(name = "quando admin usa {0} em categoria inexistente, então retorna 404")
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void inexistente(String metodo) {
        esperar(admin.enviar(metodo, "/categorias/99999999", metodo.equals("PUT") ? Map.of("nome", "Nova") : null), 404);
    }

    @Test @DisplayName("it quando admin escreve sem CSRF, então retorna 403 sem persistir")
    void semCsrf() {
        esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "Nova"), false, Map.of()), 403);
        assertThat(contar("categorias")).isZero();
    }
}
