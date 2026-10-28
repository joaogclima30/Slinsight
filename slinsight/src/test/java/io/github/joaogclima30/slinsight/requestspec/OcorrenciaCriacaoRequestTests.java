package io.github.joaogclima30.slinsight.requestspec;

import io.github.joaogclima30.slinsight.suporte.RequestSpecSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("describe criação atômica e idempotente de ocorrências")
class OcorrenciaCriacaoRequestTests extends RequestSpecSupport {
    Cliente autor, outro, admin;
    long autorId, outroId, cat, loc;
    @BeforeEach void preparar() {
        autorId = usuario("autor@example.com", "CIDADAO"); outroId = usuario("outro@example.com", "CIDADAO");
        usuario("admin@example.com", "ADMIN");
        autor = login("autor@example.com"); outro = login("outro@example.com"); admin = login("admin@example.com");
        cat = categoria(admin); loc = localizacao(autor);
    }

    @Test @DisplayName("it quando cria tentando informar outro autor, então usa o autenticado e grava ABERTA com um histórico")
    void criacaoAtomica() {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.put("usuarioId", outroId);
        var r = esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), 201);
        assertThat(((Number) objeto(r).get("usuarioId")).longValue()).isEqualTo(autorId);
        assertThat(objeto(r)).containsEntry("status", "ABERTA");
        assertThat(contar("ocorrencias")).isEqualTo(1); assertThat(contar("historico_status")).isEqualTo(1);
        var hist = db.queryForMap("select usuario_id,status_anterior,status_novo from historico_status where ocorrencia_id=?", id(r));
        assertThat(hist).containsEntry("usuario_id", autorId).containsEntry("status_anterior", null).containsEntry("status_novo", "ABERTA");
    }

    @ParameterizedTest(name = "quando {0} não existe, então retorna 404 sem ocorrência nem histórico")
    @ValueSource(strings = {"categoriaId", "localizacaoId"})
    void referenciaInexistente(String campo) {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.put(campo, 99999999);
        esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), 404);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @Test @DisplayName("it quando categoria está inativa, então retorna 409 sem gravação parcial")
    void categoriaInativa() {
        esperar(admin.enviar("DELETE", "/categorias/" + cat, null), 204);
        esperar(autor.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc)), 409);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @ParameterizedTest(name = "quando {0} está ausente, então retorna 400 sem gravar")
    @ValueSource(strings = {"titulo", "descricao", "categoriaId", "localizacaoId"})
    void obrigatorios(String campo) {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.remove(campo);
        esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), 400);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @ParameterizedTest(name = "quando categoriaId={0}, então retorna 400 sem gravar")
    @ValueSource(longs = {0, -1})
    void idInvalido(long valor) {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.put("categoriaId", valor);
        esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), 400);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @ParameterizedTest(name = "quando localizacaoId={0}, então retorna 400 sem gravar")
    @ValueSource(longs = {0, -1})
    void localizacaoIdInvalido(long valor) {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.put("localizacaoId", valor);
        esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), 400);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @ParameterizedTest(name = "quando {0} contém apenas espaços, então retorna 400 sem gravação parcial")
    @ValueSource(strings = {"titulo", "descricao"})
    void textoEmBranco(String campo) {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.put(campo, "   ");
        esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), 400);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @ParameterizedTest(name = "quando título tem {0} caracteres, então aplica o limite de 200")
    @ValueSource(ints = {0, 200, 201})
    void tituloLimite(int tamanho) {
        var corpo = new HashMap<>(denuncia(cat, loc)); corpo.put("titulo", "a".repeat(tamanho));
        esperar(autor.enviar("POST", "/ocorrencias/criar", corpo), tamanho == 200 ? 201 : 400);
        assertThat(contar("ocorrencias")).isEqualTo(tamanho == 200 ? 1 : 0);
    }

    @Test @DisplayName("it quando repete a criação com a mesma chave, então retorna a mesma ocorrência e um histórico")
    void idempotenciaSequencial() {
        var headers = Map.of("Idempotency-Key", UUID.randomUUID().toString());
        var a = esperar(autor.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc), true, headers), 201);
        var b = esperar(autor.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc), true, headers), 201);
        assertThat(id(b)).isEqualTo(id(a));
        assertThat(contar("ocorrencias")).isEqualTo(1); assertThat(contar("historico_status")).isEqualTo(1);
    }

    @Test @DisplayName("it quando cria duas vezes simultaneamente com a mesma chave, então persiste apenas uma ocorrência")
    void idempotenciaSimultanea() throws Exception {
        var outraSessao = login("autor@example.com"); var headers = Map.of("Idempotency-Key", UUID.randomUUID().toString());
        var respostas = simultaneas(() -> autor.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc), true, headers),
                () -> outraSessao.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc), true, headers));
        respostas.forEach(r -> esperar(r, 201));
        assertThat(id(respostas.get(0))).isEqualTo(id(respostas.get(1)));
        assertThat(contar("ocorrencias")).isEqualTo(1); assertThat(contar("historico_status")).isEqualTo(1);
    }

    @Test @DisplayName("it quando cria sem CSRF, então retorna 403 e não altera tabelas")
    void semCsrf() {
        esperar(autor.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc), false, Map.of()), 403);
        assertThat(contar("ocorrencias")).isZero(); assertThat(contar("historico_status")).isZero();
    }

    @ParameterizedTest(name = "quando cidadão usa localização alheia com idempotência={0}, " + "então retorna 403 sem persistir")
    @ValueSource(booleans = {false, true})
    void deveNegarCriacaoComLocalizacaoAlheia(boolean usarIdempotencia) {
        Map<String, String> headers = usarIdempotencia
                ? Map.of("Idempotency-Key", UUID.randomUUID().toString())
                : Map.of();

        var resposta = outro.enviar(
                "POST",
                "/ocorrencias/criar",
                denuncia(cat, loc),
                true,
                headers);

        org.junit.jupiter.api.Assertions.assertAll(
                () -> esperar(resposta, 403),
                () -> assertThat(contar("ocorrencias")).isZero(),
                () -> assertThat(contar("historico_status")).isZero(),
                () -> assertThat(contar("ocorrencia_idempotencia")).isZero());
    }

    @Test
    @DisplayName(
            "it quando administrador usa localização de outro autor, "
                    + "então permite criar a ocorrência"
    )
    void devePermitirAdministradorUsarLocalizacaoAlheia() {
        var resposta = esperar(admin.enviar("POST", "/ocorrencias/criar", denuncia(cat, loc)), 201);

        long adminId = db.queryForObject("select id from usuarios where email = ?", Long.class, "admin@example.com");

        assertThat(((Number) objeto(resposta).get("usuarioId")).longValue()).isEqualTo(adminId);

        assertThat(contar("ocorrencias")).isEqualTo(1);
        assertThat(contar("historico_status")).isEqualTo(1);
    }

    @Test
    @DisplayName("it quando reutiliza chave com outros dados, então retorna 409 sem alterar registros")
    void deveRejeitarChaveComConteudoDiferente() {
        var headers = Map.of("Idempotency-Key", UUID.randomUUID().toString());

        esperar(autor.enviar(
                "POST", "/ocorrencias/criar",
                denuncia(cat, loc), true, headers
        ), 201);

        var ocorrenciasAntes = db.queryForList(
                "select * from ocorrencias order by id");
        var historicoAntes = db.queryForList(
                "select * from historico_status order by id");
        var chavesAntes = db.queryForList(
                "select * from ocorrencia_idempotencia order by id");

        var corpoAlterado = new HashMap<>(denuncia(cat, loc));
        corpoAlterado.put("titulo", "Outra denúncia");

        esperar(autor.enviar(
                "POST", "/ocorrencias/criar",
                corpoAlterado, true, headers
        ), 409);

        assertThat(db.queryForList(
                "select * from ocorrencias order by id"))
                .isEqualTo(ocorrenciasAntes);

        assertThat(db.queryForList(
                "select * from historico_status order by id"))
                .isEqualTo(historicoAntes);

        assertThat(db.queryForList(
                "select * from ocorrencia_idempotencia order by id"))
                .isEqualTo(chavesAntes);
    }

    @Test
    @DisplayName("it quando usuários diferentes usam a mesma chave, então cria uma ocorrência para cada")
    void deveSepararChavePorUsuario() throws Exception {
        long outraLocalizacao = localizacao(outro);

        var headers = Map.of("Idempotency-Key", "chave-compartilhada");

        var respostas = simultaneas(
                () -> autor.enviar(
                        "POST", "/ocorrencias/criar",
                        denuncia(cat, loc), true, headers),
                () -> outro.enviar(
                        "POST", "/ocorrencias/criar",
                        denuncia(cat, outraLocalizacao), true, headers)
        );

        respostas.forEach(resposta -> esperar(resposta, 201));

        assertThat(id(respostas.get(0)))
                .isNotEqualTo(id(respostas.get(1)));

        assertThat(((Number) objeto(respostas.get(0))
                .get("usuarioId")).longValue()).isEqualTo(autorId);

        assertThat(((Number) objeto(respostas.get(1))
                .get("usuarioId")).longValue()).isEqualTo(outroId);

        assertThat(contar("ocorrencias")).isEqualTo(2);
        assertThat(contar("historico_status")).isEqualTo(2);
        assertThat(contar("ocorrencia_idempotencia")).isEqualTo(2);
    }

    @ParameterizedTest(
            name = "quando chave tem {0} caracteres, então retorna 400 sem persistir"
    )
    @ValueSource(ints = {0, 101})
    void deveRejeitarTamanhoInvalidoDaChave(int tamanho) {
        var headers = Map.of("Idempotency-Key", "a".repeat(tamanho));

        esperar(autor.enviar(
                "POST", "/ocorrencias/criar",
                denuncia(cat, loc), true, headers
        ), 400);

        assertThat(contar("ocorrencias")).isZero();
        assertThat(contar("historico_status")).isZero();
        assertThat(contar("ocorrencia_idempotencia")).isZero();
    }

    @Test
    @DisplayName("it quando chave tem 100 caracteres, então cria e reconhece a repetição")
    void deveAceitarChaveNoLimite() {
        var headers = Map.of("Idempotency-Key", "a".repeat(100));

        var primeira = esperar(autor.enviar(
                "POST", "/ocorrencias/criar",
                denuncia(cat, loc), true, headers
        ), 201);

        var repetida = esperar(autor.enviar(
                "POST", "/ocorrencias/criar",
                denuncia(cat, loc), true, headers
        ), 201);

        assertThat(id(repetida)).isEqualTo(id(primeira));
        assertThat(contar("ocorrencias")).isEqualTo(1);
        assertThat(contar("historico_status")).isEqualTo(1);
        assertThat(contar("ocorrencia_idempotencia")).isEqualTo(1);
    }

    @Test
    @DisplayName("it quando corrige uma tentativa negada, então pode reutilizar a chave")
    void tentativaNegadaNaoDeveConsumirChave() {
        var headers = Map.of("Idempotency-Key", UUID.randomUUID().toString());

        // A localização loc pertence ao autor, não ao outro cidadão.
        esperar(outro.enviar(
                "POST", "/ocorrencias/criar",
                denuncia(cat, loc), true, headers
        ), 403);

        assertThat(contar("ocorrencias")).isZero();
        assertThat(contar("historico_status")).isZero();
        assertThat(contar("ocorrencia_idempotencia")).isZero();

        long localizacaoPropria = localizacao(outro);

        var criada = esperar(outro.enviar(
                "POST", "/ocorrencias/criar",
                denuncia(cat, localizacaoPropria), true, headers
        ), 201);

        assertThat(((Number) objeto(criada).get("usuarioId")).longValue())
                .isEqualTo(outroId);

        assertThat(contar("ocorrencias")).isEqualTo(1);
        assertThat(contar("historico_status")).isEqualTo(1);
        assertThat(contar("ocorrencia_idempotencia")).isEqualTo(1);
    }
}
