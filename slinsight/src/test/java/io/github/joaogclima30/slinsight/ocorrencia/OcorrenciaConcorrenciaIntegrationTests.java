package io.github.joaogclima30.slinsight.ocorrencia;

import io.github.joaogclima30.slinsight.suporte.AutenticacaoTeste;

import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.ocorrencia.dto.AlterarStatusRequest;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaRequest;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.repository.OcorrenciaRepository;
import io.github.joaogclima30.slinsight.ocorrencia.service.OcorrenciaService;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OcorrenciaConcorrenciaIntegrationTests {

    @Autowired
    private OcorrenciaService service;

    @Autowired
    private OcorrenciaRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveSerializarAlteracoesERejeitarStatusRepetido() throws Exception {
        var transacao = new TransactionTemplate(transactionManager);
        Dados dados = criarDados(transacao);
        var executor = Executors.newFixedThreadPool(2);
        var bloqueioAdquirido = new CountDownLatch(1);
        var liberarPrimeira = new CountDownLatch(1);
        String segundaConexao = "slinsight-concorrencia-" + UUID.randomUUID();

        try {
            var primeira = executor.submit(() -> AutenticacaoTeste.executarComo(dados.email(), "CIDADAO", () -> transacao.execute(status -> {
                // Mantem o lock enquanto a segunda transacao tenta alterar a mesma linha.
                repository.buscarPorIdParaAtualizar(dados.ocorrenciaId()).orElseThrow();
                bloqueioAdquirido.countDown();
                aguardar(liberarPrimeira);
                return service.alterarStatus(dados.ocorrenciaId(),
                        new AlterarStatusRequest(StatusOcorrencia.EM_ANALISE,
                                "Primeira alteracao"));
            })));

            assertTrue(bloqueioAdquirido.await(10, TimeUnit.SECONDS),
                    "A primeira transacao nao adquiriu o bloqueio");

            var segunda = executor.submit(() -> AutenticacaoTeste.executarComo(dados.email(), "CIDADAO", () -> {
                try {
                    transacao.executeWithoutResult(status -> {
                        jdbc.queryForObject("select set_config('application_name', ?, true)",
                                String.class, segundaConexao);
                        // Limita a espera somente nesta conexao do teste.
                        jdbc.queryForObject("select set_config('lock_timeout', '15s', true)",
                                String.class);
                        service.alterarStatus(dados.ocorrenciaId(),
                                new AlterarStatusRequest(StatusOcorrencia.EM_ANALISE,
                                        "Segunda alteracao"));
                    });
                    return 200;
                } catch (ResponseStatusException exception) {
                    return exception.getStatusCode().value();
                }
            }));

            // Confirma a espera real no PostgreSQL, sem depender de um sleep fixo.
            esperarBloqueioNoPostgres(segundaConexao);
            assertFalse(segunda.isDone(), "A segunda alteracao deveria aguardar o lock");
            liberarPrimeira.countDown();

            assertEquals(StatusOcorrencia.EM_ANALISE,
                    primeira.get(20, TimeUnit.SECONDS).status());
            assertEquals(409, segunda.get(20, TimeUnit.SECONDS).intValue());
            assertEquals(StatusOcorrencia.EM_ANALISE,
                    AutenticacaoTeste.executarComo(dados.email(), "CIDADAO", () -> service.buscarPorId(dados.ocorrenciaId()).status()));

            var historico = jdbc.queryForList("""
                    select status_anterior, status_novo, usuario_id, observacao
                    from historico_status where ocorrencia_id = ?
                    order by alterado_em, id
                    """, dados.ocorrenciaId());

            assertEquals(2, historico.size());
            assertNull(historico.get(0).get("status_anterior"));
            assertEquals("ABERTA", historico.get(0).get("status_novo"));
            assertEquals("ABERTA", historico.get(1).get("status_anterior"));
            assertEquals("EM_ANALISE", historico.get(1).get("status_novo"));
            assertEquals(dados.usuarioId(), historico.get(1).get("usuario_id"));
            assertEquals("Primeira alteracao", historico.get(1).get("observacao"));
        } finally {
            liberarPrimeira.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(25, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                        "As transacoes do teste nao terminaram; limpeza nao executada");
            }
            limparDados(transacao, dados);
        }
    }

    private Dados criarDados(TransactionTemplate transacao) {
        return transacao.execute(status -> {
            String identificador = UUID.randomUUID().toString();
            Usuario usuario = new Usuario();
            usuario.setNome("Teste de concorrencia");
            usuario.setEmail(identificador + "@example.test");
            usuario.setSenhaHash("hash-ficticio-apenas-para-teste");
            entityManager.persist(usuario);

            Categoria categoria = new Categoria();
            categoria.setNome("Concorrencia " + identificador);
            entityManager.persist(categoria);

            Localizacao localizacao = new Localizacao();
            localizacao.setCidade("Sao Paulo");
            localizacao.setUf("SP");
            localizacao.setAutor(usuario);
            entityManager.persist(localizacao);

            var ocorrencia = AutenticacaoTeste.executarComo(usuario.getEmail(), usuario.getPerfil().name(), () -> service.criar(new OcorrenciaRequest(
                    "Teste de concorrencia", "Duas alteracoes simultaneas",
                    categoria.getId(), localizacao.getId())));
            return new Dados(ocorrencia.id(), usuario.getId(), categoria.getId(), localizacao.getId(), usuario.getEmail());
        });
    }

    private void esperarBloqueioNoPostgres(String nomeConexao) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limite) {
            Boolean bloqueada = jdbc.queryForObject("""
                    select exists (
                        select 1 from pg_stat_activity
                        where application_name = ? and wait_event_type = 'Lock'
                    )
                    """, Boolean.class, nomeConexao);
            if (Boolean.TRUE.equals(bloqueada)) {
                return;
            }
            Thread.sleep(50);
        }
        fail("A segunda transacao nao ficou bloqueada no PostgreSQL");
    }

    private static void aguardar(CountDownLatch sinal) {
        try {
            if (!sinal.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo excedido aguardando liberacao do teste");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Teste interrompido", exception);
        }
    }

    private void limparDados(TransactionTemplate transacao, Dados dados) {
        // Remove exclusivamente os registros criados por esta execucao, em ordem de FK.
        transacao.executeWithoutResult(status -> {
            jdbc.update("delete from historico_status where ocorrencia_id = ?", dados.ocorrenciaId());
            jdbc.update("delete from ocorrencias where id = ?", dados.ocorrenciaId());
            jdbc.update("delete from localizacoes where id = ?", dados.localizacaoId());
            jdbc.update("delete from categorias where id = ?", dados.categoriaId());
            jdbc.update("delete from usuarios where id = ?", dados.usuarioId());
        });
    }

    private record Dados(Long ocorrenciaId, Long usuarioId,
                         Long categoriaId, Long localizacaoId, String email) {
    }
}
