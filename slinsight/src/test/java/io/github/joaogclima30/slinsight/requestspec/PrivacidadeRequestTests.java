package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe isolamento entre cidadãos e privilégios administrativos")
class PrivacidadeRequestTests extends RequestSpecSupport {
    Cliente autor, outro, admin;
    long autorId, outroId, adminId, categoriaId, localId, ocorrenciaId;

    @BeforeEach void preparar() {
        autorId = usuario("autor@example.com", "CIDADAO");
        outroId = usuario("outro@example.com", "CIDADAO");
        adminId = usuario("admin@example.com", "ADMIN");
        autor = login("autor@example.com"); outro = login("outro@example.com"); admin = login("admin@example.com");
        categoriaId = categoria(admin); localId = localizacao(autor); ocorrenciaId = ocorrencia(autor, categoriaId, localId);
    }

    @Test @DisplayName("it quando o cidadão consulta seus dados e histórico, então permite sem expor senha")
    void proprio() {
        for (String rota : new String[]{"/usuarios/" + autorId, "/ocorrencias/" + ocorrenciaId,
                "/localizacoes/" + localId, "/ocorrencias/" + ocorrenciaId + "/historico"}) {
            var r = esperar(autor.enviar("GET", rota, null), 200);
            assertThat(r.body()).doesNotContain("senhaHash", "senha_hash", SENHA);
        }
    }

    @ParameterizedTest(name = "quando outro cidadão acessa {0}, então retorna 403 sem revelar dados")
    @ValueSource(strings = {"usuario", "ocorrencia", "historico", "localizacao", "filtro"})
    void negarTerceiro(String tipo) {
        String rota = switch (tipo) {
            case "usuario" -> "/usuarios/" + autorId;
            case "ocorrencia" -> "/ocorrencias/" + ocorrenciaId;
            case "historico" -> "/ocorrencias/" + ocorrenciaId + "/historico";
            case "localizacao" -> "/localizacoes/" + localId;
            default -> "/ocorrencias/listar?usuarioId=" + autorId;
        };
        var r = esperar(outro.enviar("GET", rota, null), 403);
        assertThat(r.body()).doesNotContain("autor@example.com", "Praça da Sé", "Poste apagado");
    }

    @Test @DisplayName("it quando consulta cadastro alheio inexistente, então retorna 403 sem permitir enumerar usuários")
    void estrangeiroInexistente() { esperar(outro.enviar("GET", "/usuarios/99999999", null), 403); }

    @Test @DisplayName("it quando admin consulta cadastro, ocorrência, localização e histórico alheios, então permite")
    void administrador() {
        var usuario = esperar(admin.enviar("GET", "/usuarios/" + autorId, null), 200);
        assertThat(objeto(usuario)).containsEntry("email", "autor@example.com").containsEntry("nome", "Autor autor@example.com");
        esperar(admin.enviar("GET", "/usuarios/99999999", null), 404);
        esperar(admin.enviar("GET", "/ocorrencias/" + ocorrenciaId, null), 200);
        esperar(admin.enviar("GET", "/localizacoes/" + localId, null), 200);
        esperar(admin.enviar("GET", "/ocorrencias/" + ocorrenciaId + "/historico", null), 200);
    }

    @Test @DisplayName("it quando lista sem filtro, então cidadão recebe somente suas ocorrências")
    void listaPropria() {
        long outraLocal = localizacao(outro); long outraOcorrencia = ocorrencia(outro, categoriaId, outraLocal);
        var r = esperar(outro.enviar("GET", "/ocorrencias/listar", null), 200);
        assertThat(((Number) objeto(r).get("totalElementos")).intValue()).isEqualTo(1);
        assertThat(r.body()).contains("\"id\":" + outraOcorrencia).doesNotContain("autor@example.com");
        assertThat(contar("ocorrencias")).isEqualTo(2);
    }

    @Test @DisplayName("it quando perde perfil ADMIN durante a sessão, então vira cidadão sem poder escrever categorias")
    void rebaixamentoImediato() {
        db.update("update usuarios set perfil='CIDADAO' where id=?", adminId);
        esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "Não permitido")), 403);
        assertThat(contar("categorias")).isEqualTo(1);
        assertThat(objeto(esperar(admin.enviar("GET", "/usuarios/me", null), 200))).containsEntry("perfil", "CIDADAO");
    }

    @Test @DisplayName("it quando admin perde perfil, então me e listagem refletem cidadão na mesma sessão")
    void rebaixamentoConsulta() {
        db.update("update usuarios set perfil='CIDADAO' where id=?", adminId);
        assertThat(objeto(esperar(admin.enviar("GET", "/usuarios/me", null), 200))).containsEntry("perfil", "CIDADAO");
        var lista = esperar(admin.enviar("GET", "/ocorrencias/listar", null), 200);
        assertThat(((Number) objeto(lista).get("totalElementos")).longValue()).isZero();
        esperar(admin.enviar("GET", "/ocorrencias/" + ocorrenciaId, null), 403);
    }

    @Test @DisplayName("it quando admin perde perfil, então não pode mudar status usando a sessão antiga")
    void rebaixamentoStatus() {
        db.update("update usuarios set perfil='CIDADAO' where id=?", adminId);
        var antes = db.queryForList("select * from historico_status order by id");
        esperar(admin.enviar("PATCH", "/ocorrencias/" + ocorrenciaId + "/status", Map.of("status", "EM_ANALISE")), 403);
        assertThat(db.queryForList("select * from historico_status order by id")).isEqualTo(antes);
    }

    @Test @DisplayName("it quando desativa uma conta já autenticada, então bloqueia a próxima consulta")
    void desativado() {
        db.update("update usuarios set ativo=false where id=?", autorId);
        esperar(autor.enviar("GET", "/usuarios/me", null), 403);
    }

    @ParameterizedTest(name = "quando anônimo acessa {0}, então exige autenticação")
    @ValueSource(strings = {"/usuarios/me", "/ocorrencias/listar", "/categorias/listar", "/localizacoes/1"})
    void anonimoBloqueado(String rota) { esperar(anonimo().enviar("GET", rota, null), 401); }

    @Test @DisplayName("it quando repete consultas, então não altera registros nem histórico")
    void leituraSemEfeito() {
        var antes = db.queryForList("select * from historico_status order by id");
        for (int i = 0; i < 2; i++) esperar(autor.enviar("GET", "/ocorrencias/" + ocorrenciaId, null), 200);
        assertThat(db.queryForList("select * from historico_status order by id")).isEqualTo(antes);
    }
}
