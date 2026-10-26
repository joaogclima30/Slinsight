package io.github.joaogclima30.slinsight;

import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.historico.repository.HistoricoStatusRepository;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.ocorrencia.dto.AlterarStatusRequest;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaRequest;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.service.OcorrenciaService;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OcorrenciaStatusIntegrationTests {

    @Autowired
    private OcorrenciaService service;

    @Autowired
    private HistoricoStatusRepository historicoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveCriarOcorrenciaERegistrarMudancaDeStatus() {
        String identificador = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(identificador + "@example.test");
        usuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(usuario);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("São Paulo");
        localizacao.setUf("SP");
        entityManager.persist(localizacao);

        var criada = service.criar(new OcorrenciaRequest(
                "Poste apagado",
                "Iluminação apagada há três dias",
                usuario.getId(),
                categoria.getId(),
                localizacao.getId()
        ));

        assertEquals(StatusOcorrencia.ABERTA, criada.status());

        var atualizada = service.alterarStatus(
                criada.id(),
                new AlterarStatusRequest(
                        StatusOcorrencia.EM_ANALISE,
                        usuario.getId(),
                        "Recebida para análise"
                )
        );

        entityManager.flush();
        entityManager.clear();

        assertEquals(
                StatusOcorrencia.EM_ANALISE,
                service.buscarPorId(criada.id()).status()
        );
        assertNotNull(atualizada.atualizadoEm());

        var pagina = historicoRepository.findByOcorrencia_Id(
                criada.id(),
                PageRequest.of(
                        0,
                        10,
                        Sort.by("alteradoEm", "id").ascending()
                )
        );

        assertEquals(2L, pagina.getTotalElements());

        var inicial = pagina.getContent().get(0);
        assertNull(inicial.getStatusAnterior());
        assertEquals(StatusOcorrencia.ABERTA, inicial.getStatusNovo());

        var mudanca = pagina.getContent().get(1);
        assertEquals(StatusOcorrencia.ABERTA, mudanca.getStatusAnterior());
        assertEquals(StatusOcorrencia.EM_ANALISE, mudanca.getStatusNovo());
        assertEquals(usuario.getId(), mudanca.getUsuario().getId());
        assertEquals("Recebida para análise", mudanca.getObservacao());

        var erro = assertThrows(
                ResponseStatusException.class,
                () -> service.alterarStatus(
                        criada.id(),
                        new AlterarStatusRequest(
                                StatusOcorrencia.EM_ANALISE,
                                usuario.getId(),
                                "Tentativa repetida"
                        )
                )
        );

        assertEquals(409, erro.getStatusCode().value());
    }

    @Test
    void deveRejeitarTransicaoProibidaSemAlterarOcorrenciaOuHistorico() {
        String identificador = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(identificador + "@example.test");
        usuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(usuario);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("São Paulo");
        localizacao.setUf("SP");
        entityManager.persist(localizacao);

        var criada = service.criar(new OcorrenciaRequest(
                "Poste apagado",
                "Iluminação apagada há três dias",
                usuario.getId(),
                categoria.getId(),
                localizacao.getId()
        ));

        entityManager.flush();
        entityManager.clear();
        var antes = service.buscarPorId(criada.id());

        // ABERTA não pode ir diretamente para RESOLVIDA.
        var erro = assertThrows(
                ResponseStatusException.class,
                () -> service.alterarStatus(
                        criada.id(),
                        new AlterarStatusRequest(
                                StatusOcorrencia.RESOLVIDA,
                                usuario.getId(),
                                "Tentativa de pular etapas")));

        assertEquals(409, erro.getStatusCode().value());

        entityManager.flush();
        entityManager.clear();

        var encontrada = service.buscarPorId(criada.id());

        assertEquals(StatusOcorrencia.ABERTA, encontrada.status());
        assertEquals(antes.atualizadoEm(), encontrada.atualizadoEm());

        var historico = historicoRepository.findByOcorrencia_Id(
                criada.id(),
                PageRequest.of(0, 10));

        assertEquals(1L, historico.getTotalElements());
        assertNull(historico.getContent().get(0).getStatusAnterior());
        assertEquals(
                StatusOcorrencia.ABERTA,
                historico.getContent().get(0).getStatusNovo());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOcorrencia.class, names = {"RESOLVIDA", "CANCELADA"})
    void naoDeveReabrirOcorrenciaFinalizada(StatusOcorrencia statusFinal) {
        String identificador = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(identificador + "@example.test");
        usuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(usuario);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("São Paulo");
        localizacao.setUf("SP");
        entityManager.persist(localizacao);

        var criada = service.criar(new OcorrenciaRequest(
                "Poste apagado",
                "Iluminação apagada há três dias",
                usuario.getId(),
                categoria.getId(),
                localizacao.getId()));

        // Para resolver, percorremos as etapas obrigatórias.
        if (statusFinal == StatusOcorrencia.RESOLVIDA) {
            service.alterarStatus(criada.id(), new AlterarStatusRequest(
                    StatusOcorrencia.EM_ANALISE,
                    usuario.getId(),
                    "Em análise"));

            service.alterarStatus(criada.id(), new AlterarStatusRequest(
                    StatusOcorrencia.EM_ANDAMENTO,
                    usuario.getId(),
                    "Em atendimento"));
        }

        service.alterarStatus(criada.id(), new AlterarStatusRequest(
                statusFinal,
                usuario.getId(),
                "Ocorrência finalizada"));

        entityManager.flush();
        entityManager.clear();

        var antes = service.buscarPorId(criada.id());

        long quantidadeAntes = historicoRepository.findByOcorrencia_Id(criada.id(), PageRequest.of(0, 10)).getTotalElements();

        var erro = assertThrows(
                ResponseStatusException.class,
                () -> service.alterarStatus(
                        criada.id(),
                        new AlterarStatusRequest(
                                StatusOcorrencia.ABERTA,
                                usuario.getId(),
                                "Tentativa de reabertura")));

        assertEquals(409, erro.getStatusCode().value());

        entityManager.flush();
        entityManager.clear();

        var depois = service.buscarPorId(criada.id());

        assertEquals(statusFinal, depois.status());
        assertEquals(antes.atualizadoEm(), depois.atualizadoEm());

        long quantidadeDepois = historicoRepository.findByOcorrencia_Id(
                criada.id(),
                PageRequest.of(0, 10)).getTotalElements();

        assertEquals(quantidadeAntes, quantidadeDepois);
    }

    @Test
    void deveFiltrarPorUsuarioCategoriaEStatus() {
        String identificador = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(identificador + "@example.test");
        usuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(usuario);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("São Paulo");
        localizacao.setUf("SP");
        entityManager.persist(localizacao);

        var primeira = service.criar(new OcorrenciaRequest(
                "Primeira ocorrência",
                "Deve aparecer no filtro",
                usuario.getId(),
                categoria.getId(),
                localizacao.getId()));

        var segunda = service.criar(new OcorrenciaRequest(
                "Segunda ocorrência",
                "Será movida para análise",
                usuario.getId(),
                categoria.getId(),
                localizacao.getId()));

        service.alterarStatus(
                segunda.id(),
                new AlterarStatusRequest(
                        StatusOcorrencia.EM_ANALISE,
                        usuario.getId(),
                        "Em análise"));

        Usuario outroUsuario = new Usuario();
        outroUsuario.setNome("Outro usuário");
        outroUsuario.setEmail(UUID.randomUUID() + "@example.test");
        outroUsuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(outroUsuario);

        Categoria outraCategoria = new Categoria();
        outraCategoria.setNome("Outra categoria " + UUID.randomUUID());
        entityManager.persist(outraCategoria);

        var deOutroUsuario = service.criar(new OcorrenciaRequest(
                "Ocorrência de outro usuário",
                "Não deve aparecer no filtro combinado",
                outroUsuario.getId(),
                categoria.getId(),
                localizacao.getId()
        ));


        var deOutraCategoria = service.criar(new OcorrenciaRequest(
                "Ocorrência de outra categoria",
                "Não deve aparecer no filtro combinado",
                usuario.getId(),
                outraCategoria.getId(),
                localizacao.getId()
        ));

        entityManager.flush();
        entityManager.clear();

        var abertas = service.listar(
                usuario.getId(),
                categoria.getId(),
                StatusOcorrencia.ABERTA,
                PageRequest.of(0, 10));

        assertEquals(1L, abertas.getTotalElements());
        assertEquals(primeira.id(), abertas.getContent().get(0).id());

        assertTrue(abertas.getContent().stream()
                .noneMatch(ocorrencia ->
                        ocorrencia.id().equals(deOutroUsuario.id())
                                || ocorrencia.id().equals(deOutraCategoria.id())
                ));

        var emAnalise = service.listar(
                usuario.getId(),
                categoria.getId(),
                StatusOcorrencia.EM_ANALISE,
                PageRequest.of(0, 10));

        assertEquals(1L, emAnalise.getTotalElements());
        assertEquals(segunda.id(), emAnalise.getContent().get(0).id());

        var resolvidas = service.listar(
                usuario.getId(),
                categoria.getId(),
                StatusOcorrencia.RESOLVIDA,
                PageRequest.of(0, 10));

        assertTrue(resolvidas.isEmpty());

        // Somente usuário: primeira, segunda e deOutraCategoria.
        var porUsuario = service.listar(
                usuario.getId(),
                null,
                null,
                PageRequest.of(0, 10));

        assertEquals(3L, porUsuario.getTotalElements());

        assertEquals(
                java.util.Set.of(
                        primeira.id(),
                        segunda.id(),
                        deOutraCategoria.id()
                ),
                porUsuario.getContent().stream()
                        .map(ocorrencia -> ocorrencia.id())
                        .collect(java.util.stream.Collectors.toSet()));

// Somente categoria: primeira, segunda e deOutroUsuario.
        var porCategoria = service.listar(
                null,
                categoria.getId(),
                null,
                PageRequest.of(0, 10));

        assertEquals(3L, porCategoria.getTotalElements());

        assertEquals(
                java.util.Set.of(
                        primeira.id(),
                        segunda.id(),
                        deOutroUsuario.id()),
                porCategoria.getContent().stream()
                        .map(ocorrencia -> ocorrencia.id())
                        .collect(java.util.stream.Collectors.toSet()));

// Usuário + status, sem filtro de categoria.
        var abertasDoUsuario = service.listar(
                usuario.getId(),
                null,
                StatusOcorrencia.ABERTA,
                PageRequest.of(0, 10));

        assertEquals(2L, abertasDoUsuario.getTotalElements());

        assertEquals(
                java.util.Set.of(primeira.id(), deOutraCategoria.id()),
                abertasDoUsuario.getContent().stream()
                        .map(ocorrencia -> ocorrencia.id())
                        .collect(java.util.stream.Collectors.toSet()));

        var ordenacao = Sort.by("id").ascending();

        var paginaZero = service.listar(
                usuario.getId(),
                null,
                null,
                PageRequest.of(0, 2, ordenacao)
        );

        var paginaUm = service.listar(
                usuario.getId(),
                null,
                null,
                PageRequest.of(1, 2, ordenacao)
        );

// Existem três registros, divididos em duas páginas.
        assertEquals(3L, paginaZero.getTotalElements());
        assertEquals(2, paginaZero.getTotalPages());
        assertEquals(2, paginaZero.getNumberOfElements());
        assertTrue(paginaZero.isFirst());
        assertTrue(paginaZero.hasNext());

        assertEquals(3L, paginaUm.getTotalElements());
        assertEquals(1, paginaUm.getNumberOfElements());
        assertEquals(1, paginaUm.getNumber());
        assertTrue(paginaUm.isLast());
        assertFalse(paginaUm.hasNext());

// Confirma a ordem e a distribuição dos registros.
        var idsEsperados = java.util.stream.Stream.of(
                primeira.id(),
                segunda.id(),
                deOutraCategoria.id()).sorted().toList();

        assertEquals(
                idsEsperados.subList(0, 2),
                paginaZero.getContent().stream()
                        .map(ocorrencia -> ocorrencia.id())
                        .toList());

        assertEquals(
                idsEsperados.subList(2, 3),
                paginaUm.getContent().stream()
                        .map(ocorrencia -> ocorrencia.id())
                        .toList());

// Uma página além da última deve retornar conteúdo vazio.
        var paginaDois = service.listar(
                usuario.getId(),
                null,
                null,
                PageRequest.of(2, 2, ordenacao));

        assertTrue(paginaDois.isEmpty());
        assertEquals(3L, paginaDois.getTotalElements());
    }

    @Test
    void deveRejeitarIdsInvalidosNosFiltros() {
        for (Long idInvalido : java.util.List.of(0L, -1L)) {
            var erroUsuario = assertThrows(
                    ResponseStatusException.class,
                    () -> service.listar(
                            idInvalido,
                            null,
                            null,
                            PageRequest.of(0, 10)));

            assertEquals(400, erroUsuario.getStatusCode().value());
            assertEquals("O ID do usuário deve ser positivo", erroUsuario.getReason());

            var erroCategoria = assertThrows(
                    ResponseStatusException.class,
                    () -> service.listar(
                            null,
                            idInvalido,
                            null,
                            PageRequest.of(0, 10)));

            assertEquals(400, erroCategoria.getStatusCode().value());
            assertEquals("O ID da categoria deve ser positivo", erroCategoria.getReason());
        }
    }
}
