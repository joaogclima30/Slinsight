package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe transições de status com histórico atômico")
class OcorrenciaStatusRequestTests extends RequestSpecSupport {
    Cliente admin, autor;
    long adminId, autorId, id;
    @BeforeEach void preparar() {
        adminId = usuario("admin@example.com", "ADMIN"); autorId = usuario("autor@example.com", "CIDADAO");
        admin = login("admin@example.com"); autor = login("autor@example.com");
        id = ocorrencia(autor, categoria(admin), localizacao(autor));
    }

    private java.net.http.HttpResponse<String> mudar(Cliente c, String status) {
        return c.enviar("PATCH", "/ocorrencias/" + id + "/status", Map.of("status", status, "observacao", "Análise administrativa"));
    }
    private java.util.List<Map<String,Object>> snapshot() {
        return db.queryForList("select o.*,h.id as historico_id,h.usuario_id as responsavel,h.status_anterior,h.status_novo,h.observacao,h.alterado_em from ocorrencias o join historico_status h on h.ocorrencia_id=o.id order by h.id");
    }

    @Test @DisplayName("it quando admin segue o fluxo até resolver, então preserva autor e registra cada responsável e transição")
    void fluxoFeliz() {
        var criado = objeto(esperar(autor.enviar("GET", "/ocorrencias/" + id, null), 200));
        for (String status : new String[]{"EM_ANALISE", "EM_ANDAMENTO", "RESOLVIDA"}) {
            var r = esperar(mudar(admin, status), 200);
            assertThat(objeto(r)).containsEntry("status", status).containsEntry("criadoEm", criado.get("criadoEm"));
            assertThat(((Number) objeto(r).get("usuarioId")).longValue()).isEqualTo(autorId);
        }
        assertThat(contar("historico_status")).isEqualTo(4);
        var h = db.queryForList("select usuario_id,status_anterior,status_novo from historico_status order by id");
        assertThat(h).extracting(m -> m.get("status_novo")).containsExactly("ABERTA", "EM_ANALISE", "EM_ANDAMENTO", "RESOLVIDA");
        assertThat(h.subList(1, 4)).allSatisfy(m -> assertThat(m.get("usuario_id")).isEqualTo(adminId));
        assertThat(h).extracting(m -> m.get("status_anterior")).containsExactly(null, "ABERTA", "EM_ANALISE", "EM_ANDAMENTO");
        var datas = db.queryForMap("select criado_em,atualizado_em from ocorrencias where id=?", id);
        assertThat(((java.sql.Timestamp) datas.get("atualizado_em")).toInstant()).isAfterOrEqualTo(((java.sql.Timestamp) datas.get("criado_em")).toInstant());
    }

    @ParameterizedTest(name = "quando cancela a partir de {0}, então registra CANCELADA")
    @ValueSource(strings = {"ABERTA", "EM_ANALISE", "EM_ANDAMENTO"})
    void cancelamento(String anterior) {
        if (!anterior.equals("ABERTA")) esperar(mudar(admin, "EM_ANALISE"), 200);
        if (anterior.equals("EM_ANDAMENTO")) esperar(mudar(admin, "EM_ANDAMENTO"), 200);
        esperar(mudar(admin, "CANCELADA"), 200);
        assertThat(db.queryForObject("select status from ocorrencias where id=?", String.class, id)).isEqualTo("CANCELADA");
    }

    @ParameterizedTest(name = "quando tenta ABERTA para {0}, então retorna 409 sem alterar data ou histórico")
    @ValueSource(strings = {"ABERTA", "EM_ANDAMENTO", "RESOLVIDA"})
    void transicaoInvalida(String destino) {
        var antes = snapshot(); esperar(mudar(admin, destino), 409); assertThat(snapshot()).isEqualTo(antes);
    }

    @ParameterizedTest(name = "quando ocorrência {0} tenta ir para {1}, então retorna 409 e permanece encerrada")
    @CsvSource({"CANCELADA,ABERTA", "CANCELADA,EM_ANALISE", "CANCELADA,EM_ANDAMENTO", "CANCELADA,RESOLVIDA", "CANCELADA,CANCELADA",
            "RESOLVIDA,ABERTA", "RESOLVIDA,EM_ANALISE", "RESOLVIDA,EM_ANDAMENTO", "RESOLVIDA,CANCELADA", "RESOLVIDA,RESOLVIDA"})
    void finaisNaoReabrem(String encerrada, String destino) {
        if (encerrada.equals("RESOLVIDA")) {
            esperar(mudar(admin, "EM_ANALISE"), 200); esperar(mudar(admin, "EM_ANDAMENTO"), 200);
        }
        esperar(mudar(admin, encerrada), 200);
        var antes = snapshot(); esperar(mudar(admin, destino), 409); assertThat(snapshot()).isEqualTo(antes);
    }

    @Test @DisplayName("it quando cidadão tenta mudar status mesmo sendo autor, então retorna 403 sem efeito")
    void cidadaoNegado() {
        var antes = snapshot(); esperar(mudar(autor, "EM_ANALISE"), 403); assertThat(snapshot()).isEqualTo(antes);
    }

    @Test @DisplayName("it quando admin envia sem CSRF, então retorna 403 sem alterar histórico")
    void semCsrf() {
        var antes = snapshot();
        esperar(admin.enviar("PATCH", "/ocorrencias/" + id + "/status", Map.of("status", "EM_ANALISE"), false, Map.of()), 403);
        assertThat(snapshot()).isEqualTo(antes);
    }

    @Test @DisplayName("it quando observação tem 2000 caracteres e tenta forjar responsável, então aceita e usa o admin autenticado")
    void observacaoLimiteResponsavel() {
        esperar(admin.enviar("PATCH", "/ocorrencias/" + id + "/status",
                Map.of("status", "EM_ANALISE", "observacao", "a".repeat(2000), "usuarioId", autorId)), 200);
        var h = db.queryForMap("select usuario_id,observacao from historico_status order by id desc limit 1");
        assertThat(h).containsEntry("usuario_id", adminId).containsEntry("observacao", "a".repeat(2000));
    }

    @Test @DisplayName("it quando admin muda status inexistente, então retorna 404 sem gravação")
    void inexistente() {
        var antes = snapshot();
        esperar(admin.enviar("PATCH", "/ocorrencias/99999999/status", Map.of("status", "EM_ANALISE")), 404);
        assertThat(snapshot()).isEqualTo(antes);
    }

    @ParameterizedTest(name = "quando corpo é {0}, então retorna 400 sem efeito")
    @ValueSource(strings = {"json", "ausente", "status", "observacao"})
    void corpoInvalido(String caso) {
        var antes = snapshot();
        String corpo = switch(caso) {
            case "json" -> "{invalido";
            case "ausente" -> null;
            case "status" -> "{\"status\":\"INEXISTENTE\"}";
            default -> JSON.writeValueAsString(Map.of("status", "EM_ANALISE", "observacao", "a".repeat(2001)));
        };
        esperar(admin.requisitar("PATCH", "/ocorrencias/" + id + "/status", corpo, "application/json", true, Map.of()), 400);
        assertThat(snapshot()).isEqualTo(antes);
    }

    @Test @DisplayName("it quando duas requisições mudam para o mesmo status simultaneamente, então uma vence e outra recebe 409")
    void mesmoDestinoConcorrente() throws Exception {
        var outraSessao = login("admin@example.com");
        var resultados = simultaneas(() -> mudar(admin, "EM_ANALISE"), () -> mudar(outraSessao, "EM_ANALISE"));
        assertThat(resultados).extracting(r -> r.statusCode()).containsExactlyInAnyOrder(200, 409);
        assertThat(contar("historico_status")).isEqualTo(2);
        assertThat(db.queryForObject("select status from ocorrencias where id=?", String.class, id)).isEqualTo("EM_ANALISE");
    }

    @Test @DisplayName("it quando análise e cancelamento concorrem, então serializa as mudanças sem perder histórico")
    void destinosDiferentesConcorrentes() throws Exception {
        var outraSessao = login("admin@example.com");
        var resultados = simultaneas(() -> mudar(admin, "EM_ANALISE"), () -> mudar(outraSessao, "CANCELADA"));
        esperar(resultados.get(1), 200);
        assertThat(resultados.get(0).statusCode()).isIn(200, 409);
        var hist = db.queryForList("select status_anterior,status_novo from historico_status order by id");
        assertThat(hist).hasSize(resultados.get(0).statusCode() == 200 ? 3 : 2);
        for (int i = 1; i < hist.size(); i++) assertThat(hist.get(i).get("status_anterior")).isEqualTo(hist.get(i - 1).get("status_novo"));
        assertThat(hist.getLast().get("status_novo")).isEqualTo("CANCELADA");
        assertThat(db.queryForObject("select status from ocorrencias where id=?", String.class, id)).isEqualTo("CANCELADA");
    }
}
