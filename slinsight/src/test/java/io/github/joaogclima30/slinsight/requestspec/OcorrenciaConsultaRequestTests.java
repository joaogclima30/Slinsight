package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe consultas com filtros e paginação")
class OcorrenciaConsultaRequestTests extends RequestSpecSupport {
    Cliente admin, autor;
    long autorId, cat, outraCat, primeira, segunda, terceira;
    @BeforeEach void preparar() {
        usuario("admin@example.com", "ADMIN"); autorId = usuario("autor@example.com", "CIDADAO");
        usuario("outro@example.com", "CIDADAO");
        admin = login("admin@example.com"); autor = login("autor@example.com"); var outro = login("outro@example.com");
        cat = categoria(admin);
        outraCat = id(esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "Buracos")), 201));
        long local = localizacao(autor);
        primeira = ocorrencia(autor, cat, local); segunda = ocorrencia(autor, outraCat, local);
        terceira = ocorrencia(outro, cat, localizacao(outro));
        esperar(admin.enviar("PATCH", "/ocorrencias/" + primeira + "/status", Map.of("status", "EM_ANALISE")), 200);
    }

    @Test @DisplayName("it quando combina usuário, categoria e status, então aplica os filtros com AND")
    void filtrosCombinados() {
        var r = esperar(admin.enviar("GET", "/ocorrencias/listar?usuarioId=" + autorId + "&categoriaId=" + cat + "&status=EM_ANALISE", null), 200);
        assertThat(total(r)).isEqualTo(1);
        assertThat(ids(r)).containsExactly(primeira);
    }

    @Test @DisplayName("it quando filtros não encontram registros, então retorna 200 com página vazia")
    void semResultado() {
        var r = esperar(admin.enviar("GET", "/ocorrencias/listar?categoriaId=" + outraCat + "&status=RESOLVIDA", null), 200);
        assertThat(total(r)).isZero(); assertThat(ids(r)).isEmpty();
    }

    @Test @DisplayName("it quando navega páginas com datas iguais, então mantém ordem estável sem repetir nem perder registros")
    void paginacaoEstavel() {
        db.update("update ocorrencias set criado_em='2026-01-01T00:00:00Z'");
        var a = esperar(admin.enviar("GET", "/ocorrencias/listar?page=0&size=2", null), 200);
        var b = esperar(admin.enviar("GET", "/ocorrencias/listar?page=1&size=2", null), 200);
        assertThat(ids(a)).containsExactly(terceira, segunda); assertThat(ids(b)).containsExactly(primeira);
        assertThat(total(a)).isEqualTo(3); assertThat(total(b)).isEqualTo(3);
    }

    @ParameterizedTest(name = "quando consulta com {0}, então retorna 400")
    @ValueSource(strings = {"page=-1", "size=0", "size=-1", "size=101", "usuarioId=0", "usuarioId=-1", "categoriaId=0", "status=INEXISTENTE"})
    void parametrosInvalidos(String parametro) {
        esperar(admin.enviar("GET", "/ocorrencias/listar?" + parametro, null), 400);
        assertThat(contar("ocorrencias")).isEqualTo(3);
    }

    @ParameterizedTest(name = "quando size={0}, então aceita dentro do limite de cem")
    @ValueSource(ints = {1, 100})
    void tamanhosValidos(int tamanho) {
        var r = esperar(admin.enviar("GET", "/ocorrencias/listar?size=" + tamanho, null), 200);
        assertThat(((Number) objeto(r).get("tamanho")).intValue()).isEqualTo(tamanho);
    }

    @ParameterizedTest(name = "quando histórico recebe {0}, então retorna 400")
    @ValueSource(strings = {"page=-1", "size=0", "size=-1", "size=101"})
    void paginacaoHistoricoInvalida(String parametro) {
        esperar(autor.enviar("GET", "/ocorrencias/" + primeira + "/historico?" + parametro, null), 400);
    }

    @Test @DisplayName("it quando autor consulta histórico, então retorna criação antes da mudança de status")
    void historicoOrdenado() {
        var r = esperar(autor.enviar("GET", "/ocorrencias/" + primeira + "/historico", null), 200);
        @SuppressWarnings("unchecked") var registros = (List<Map<String,Object>>) objeto(r).get("content");
        assertThat(registros).extracting(h -> h.get("statusNovo")).containsExactly("ABERTA", "EM_ANALISE");
    }

    private long total(java.net.http.HttpResponse<String> r) { return ((Number) objeto(r).get("totalElementos")).longValue(); }
    @SuppressWarnings("unchecked")
    private List<Long> ids(java.net.http.HttpResponse<String> r) {
        return ((List<Map<String,Object>>) objeto(r).get("content")).stream().map(m -> ((Number)m.get("id")).longValue()).toList();
    }
}
