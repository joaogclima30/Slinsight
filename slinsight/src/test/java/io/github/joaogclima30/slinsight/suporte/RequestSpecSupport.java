package io.github.joaogclima30.slinsight.suporte;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class RequestSpecSupport {
    protected static final String SENHA = "SenhaDeTeste123!";
    protected static final JsonMapper JSON = new JsonMapper();
    @Value("${local.server.port}") private int porta;
    @Autowired protected JdbcTemplate db;
    @Autowired protected PasswordEncoder encoder;
    private final List<Cliente> clientes = new java.util.ArrayList<>();

    @AfterEach
    void fecharClientesHttp() {
        clientes.forEach(c -> c.http.close());
        clientes.clear();
    }

    @BeforeEach
    void limparSomenteBancoDescartavel() {
        assertThat(db.queryForObject("select current_database()", String.class))
                .as("Limpeza permitida somente no PostgreSQL descartável dos testes")
                .isEqualTo("slinsight_test");
        db.execute("TRUNCATE historico_status, ocorrencias, localizacoes, categorias, usuarios RESTART IDENTITY CASCADE");
    }

    protected long usuario(String email, String perfil) {
        return db.queryForObject("INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES (?,?,?,?) RETURNING id",
                Long.class, "Autor " + email, email, encoder.encode(SENHA), perfil);
    }

    protected Cliente anonimo() {
        var c = new Cliente(); clientes.add(c); return c;
    }

    protected Cliente login(String email) {
        Cliente c = anonimo();
        c.csrf();
        esperar(c.entrar(email, SENHA), 204);
        c.csrf();
        return c;
    }

    protected long categoria(Cliente admin) {
        return id(esperar(admin.enviar("POST", "/categorias/criar", Map.of("nome", "Iluminação", "descricao", "Lâmpadas")), 201));
    }

    protected Map<String, Object> endereco() {
        return Map.of("cidade", "São Paulo", "uf", "SP", "cep", "01001000",
                "logradouro", "Praça da Sé", "latitude", -23.55052, "longitude", -46.633308);
    }

    protected long localizacao(Cliente autor) {
        return id(esperar(autor.enviar("POST", "/localizacoes", endereco()), 201));
    }

    protected Map<String, Object> denuncia(long categoria, long localizacao) {
        return Map.of("titulo", "Poste apagado", "descricao", "Lâmpada apagada há três dias",
                "categoriaId", categoria, "localizacaoId", localizacao);
    }

    protected long ocorrencia(Cliente autor, long categoria, long localizacao) {
        return id(esperar(autor.enviar("POST", "/ocorrencias/criar", denuncia(categoria, localizacao)), 201));
    }

    protected static HttpResponse<String> esperar(HttpResponse<String> r, int status) {
        assertThat(r.statusCode()).as(r.request().method() + " " + r.uri() + "\n" + r.body()).isEqualTo(status);
        return r;
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> objeto(HttpResponse<String> r) { return JSON.readValue(r.body(), Map.class); }
    protected static long id(HttpResponse<String> r) { return ((Number) objeto(r).get("id")).longValue(); }
    protected int contar(String tabela) { return db.queryForObject("select count(*) from " + tabela, Integer.class); }

    protected List<HttpResponse<String>> simultaneas(Supplier<HttpResponse<String>> a, Supplier<HttpResponse<String>> b) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var pronto = new CountDownLatch(2);
            var largada = new CountDownLatch(1);
            java.util.function.Function<Supplier<HttpResponse<String>>, CompletableFuture<HttpResponse<String>>> executar = acao ->
                    CompletableFuture.supplyAsync(() -> {
                        pronto.countDown();
                        try { if (!largada.await(10, TimeUnit.SECONDS)) throw new AssertionError("Largada não liberada"); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                        return acao.get();
                    }, executor);
            var primeira = executar.apply(a);
            var segunda = executar.apply(b);
            assertThat(pronto.await(10, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            return List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
        }
    }

    protected class Cliente {
        public final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        public String token;
        String cabecalho = "X-CSRF-TOKEN";

        public void csrf() {
            var resposta = esperar(enviar("GET", "/csrf", null, false, Map.of()), 200);
            token = (String) objeto(resposta).get("token");
            cabecalho = (String) objeto(resposta).get("headerName");
            assertThat(token).isNotBlank();
        }

        public HttpResponse<String> entrar(String email, String senha) {
            String corpo = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&senha=" + URLEncoder.encode(senha, StandardCharsets.UTF_8);
            return requisitar("POST", "/auth/login", corpo, "application/x-www-form-urlencoded", true, Map.of());
        }

        public HttpResponse<String> enviar(String metodo, String caminho, Object corpo) {
            return enviar(metodo, caminho, corpo, true, Map.of());
        }

        public HttpResponse<String> enviar(String metodo, String caminho, Object corpo, boolean csrf, Map<String,String> headers) {
            return requisitar(metodo, caminho, corpo == null ? null : JSON.writeValueAsString(corpo), "application/json", csrf, headers);
        }

        public HttpResponse<String> requisitar(String metodo, String caminho, String corpo, String tipo, boolean csrf, Map<String,String> headers) {
            var r = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                    .timeout(Duration.ofSeconds(25)).header("Content-Type", tipo);
            if (csrf && token != null) r.header(cabecalho, token);
            headers.forEach(r::header);
            r.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
            try { return http.send(r.build(), HttpResponse.BodyHandlers.ofString()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            catch (java.io.IOException e) { throw new AssertionError(e); }
        }
    }
}
