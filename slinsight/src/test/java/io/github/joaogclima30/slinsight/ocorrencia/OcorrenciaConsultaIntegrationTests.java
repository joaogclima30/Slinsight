package io.github.joaogclima30.slinsight.ocorrencia;

import io.github.joaogclima30.slinsight.suporte.AutenticacaoTeste;

import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.ocorrencia.dto.AlterarStatusRequest;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaRequest;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.service.OcorrenciaService;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.historico.service.HistoricoStatusService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaResponse;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OcorrenciaConsultaIntegrationTests {

    @Autowired
    private OcorrenciaService service;

    @Autowired
    private HistoricoStatusService historicoService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveCombinarFiltrosPorUsuarioCategoriaEStatus() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var abertas = service.listar(d.usuarioId(), d.categoriaId(),
                StatusOcorrencia.ABERTA, PageRequest.of(0, 10));
        assertEquals(1L, abertas.getTotalElements());
        assertEquals(Set.of(d.primeiraId()), ids(abertas));
        assertTrue(abertas.getContent().stream().noneMatch(o ->
                o.id().equals(d.deOutroUsuarioId()) || o.id().equals(d.deOutraCategoriaId())));
        var emAnalise = service.listar(d.usuarioId(), d.categoriaId(),
                StatusOcorrencia.EM_ANALISE, PageRequest.of(0, 10));
        assertEquals(1L, emAnalise.getTotalElements());
        assertEquals(Set.of(d.segundaId()), ids(emAnalise));
        var resolvidas = service.listar(d.usuarioId(), d.categoriaId(),
                StatusOcorrencia.RESOLVIDA, PageRequest.of(0, 10));
        assertTrue(resolvidas.isEmpty());
    }

    @Test
    void deveFiltrarSomentePorUsuario() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var pagina = service.listar(d.usuarioId(), null, null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutraCategoriaId()), ids(pagina));
    }

    @Test
    void deveFiltrarSomentePorCategoria() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var pagina = service.listar(null, d.categoriaId(), null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutroUsuarioId()), ids(pagina));
    }

    @Test
    void deveFiltrarPorUsuarioEStatus() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var pagina = service.listar(d.usuarioId(), null,
                StatusOcorrencia.ABERTA, PageRequest.of(0, 10));
        assertEquals(2L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.deOutraCategoriaId()), ids(pagina));
    }

    @Test
    void devePaginarSemRepetirRegistros() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var ordenacao = Sort.by("id").ascending();
        var paginaZero = service.listar(d.usuarioId(), null, null, PageRequest.of(0, 2, ordenacao));
        var paginaUm = service.listar(d.usuarioId(), null, null, PageRequest.of(1, 2, ordenacao));
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
        var esperados = java.util.stream.Stream.of(d.primeiraId(), d.segundaId(),
                d.deOutraCategoriaId()).sorted().toList();
        assertEquals(esperados.subList(0, 2), paginaZero.getContent().stream()
                .map(OcorrenciaResponse::id).toList());
        assertEquals(esperados.subList(2, 3), paginaUm.getContent().stream()
                .map(OcorrenciaResponse::id).toList());
        var paginaDois = service.listar(d.usuarioId(), null, null, PageRequest.of(2, 2, ordenacao));
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

    @Test
    void cidadaoDeveListarApenasPropriasOcorrenciasSemFiltroDeUsuario() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        var pagina = service.listar(null, null, null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutraCategoriaId()), ids(pagina));
        assertTrue(pagina.getContent().stream().allMatch(o -> o.usuarioId().equals(d.usuarioId())));
    }

    @Test
    void cidadaoDeveFiltrarPropriasOcorrenciasPorCategoriaEStatus() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        var pagina = service.listar(null, d.categoriaId(), StatusOcorrencia.ABERTA, PageRequest.of(0, 10));
        assertEquals(1L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId()), ids(pagina));
    }

    @Test
    void cidadaoDevePoderInformarProprioIdNoFiltro() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        var pagina = service.listar(d.usuarioId(), null, null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutraCategoriaId()), ids(pagina));
    }

    @Test
    void cidadaoNaoDeveFiltrarPorOutroUsuario() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.listar(d.outroUsuarioId(), null, null, PageRequest.of(0, 10)));
        assertEquals(403, erro.getStatusCode().value());
    }

    @Test
    void cidadaoNaoDeveConsultarOcorrenciaDeOutroAutor() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.buscarPorId(d.deOutroUsuarioId()));
        assertEquals(403, erro.getStatusCode().value());
    }

    @Test
    void cidadaoNaoDeveConsultarHistoricoDeOutroAutor() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        var erro = assertThrows(ResponseStatusException.class,
                () -> historicoService.listarPorOcorrencia(d.deOutroUsuarioId(), PageRequest.of(0, 10)));
        assertEquals(403, erro.getStatusCode().value());
    }

    @Test
    void autorDeveConsultarPropriaOcorrenciaEHistorico() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.usuarioEmail(), PerfilUsuario.CIDADAO.name());
        assertEquals(d.usuarioId(), service.buscarPorId(d.primeiraId()).usuarioId());
        var historico = historicoService.listarPorOcorrencia(d.primeiraId(), PageRequest.of(0, 10));
        assertEquals(1L, historico.getTotalElements());
        assertEquals(d.primeiraId(), historico.getContent().get(0).ocorrenciaId());
        assertEquals(d.usuarioId(), historico.getContent().get(0).usuarioId());
    }

    @Test
    void administradorDeveConsultarOcorrenciaEHistoricoDeOutroUsuario() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        assertEquals(d.outroUsuarioId(), service.buscarPorId(d.deOutroUsuarioId()).usuarioId());
        var historico = historicoService.listarPorOcorrencia(d.deOutroUsuarioId(), PageRequest.of(0, 10));
        assertEquals(1L, historico.getTotalElements());
        assertEquals(d.deOutroUsuarioId(), historico.getContent().get(0).ocorrenciaId());
        var pagina = service.listar(d.outroUsuarioId(), null, null, PageRequest.of(0, 10));
        assertEquals(1L, pagina.getTotalElements());
        assertEquals(Set.of(d.deOutroUsuarioId()), ids(pagina));
    }

    @Test
    void administradorDeveListarOcorrenciasDeAutoresDiferentesSemFiltroDeUsuario() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        // Categoria exclusiva desta execucao evita depender de registros ja existentes no banco.
        var pagina = service.listar(null, d.categoriaId(), null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutroUsuarioId()), ids(pagina));
    }

    @Test
    void deveRetornar404ParaOcorrenciaInexistente() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.buscarPorId(Long.MAX_VALUE));
        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void deveRetornar404ParaHistoricoDeOcorrenciaInexistente() {
        var d = prepararDados();
        AutenticacaoTeste.autenticar(d.adminEmail(), PerfilUsuario.ADMIN.name());
        var erro = assertThrows(ResponseStatusException.class,
                () -> historicoService.listarPorOcorrencia(Long.MAX_VALUE, PageRequest.of(0, 10)));
        assertEquals(404, erro.getStatusCode().value());
    }

    private Set<Long> ids(Page<OcorrenciaResponse> pagina) {
        return pagina.getContent().stream().map(OcorrenciaResponse::id).collect(Collectors.toSet());
    }

    private DadosConsulta prepararDados() {
        String identificador = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(identificador + "@example.test");
        usuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(usuario);
        AutenticacaoTeste.autenticar(usuario.getEmail(), usuario.getPerfil().name());

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("São Paulo");
        localizacao.setUf("SP");
        localizacao.setAutor(usuario);
        entityManager.persist(localizacao);

        var primeira = service.criar(new OcorrenciaRequest(
                "Primeira ocorrência",
                "Deve aparecer no filtro",
                categoria.getId(),
                localizacao.getId()));

        var segunda = service.criar(new OcorrenciaRequest(
                "Segunda ocorrência",
                "Será movida para análise",
                categoria.getId(),
                localizacao.getId()));

        service.alterarStatus(
                segunda.id(),
                new AlterarStatusRequest(
                        StatusOcorrencia.EM_ANALISE,
                        "Em análise"));

        Usuario outroUsuario = new Usuario();
        outroUsuario.setNome("Outro usuário");
        outroUsuario.setEmail(UUID.randomUUID() + "@example.test");
        outroUsuario.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(outroUsuario);

        Localizacao outraLocalizacao = new Localizacao();
        outraLocalizacao.setCidade("São Paulo");
        outraLocalizacao.setUf("SP");
        outraLocalizacao.setAutor(outroUsuario);
        entityManager.persist(outraLocalizacao);

        Categoria outraCategoria = new Categoria();
        outraCategoria.setNome("Outra categoria " + UUID.randomUUID());
        entityManager.persist(outraCategoria);

        AutenticacaoTeste.autenticar(outroUsuario.getEmail(), outroUsuario.getPerfil().name());
        var deOutroUsuario = service.criar(new OcorrenciaRequest(
                "Ocorrência de outro usuário",
                "Não deve aparecer no filtro combinado",
                categoria.getId(),
                outraLocalizacao.getId()
        ));


        AutenticacaoTeste.autenticar(usuario.getEmail(), usuario.getPerfil().name());
        var deOutraCategoria = service.criar(new OcorrenciaRequest(
                "Ocorrência de outra categoria",
                "Não deve aparecer no filtro combinado",
                outraCategoria.getId(),
                localizacao.getId()
        ));

        Usuario administrador = new Usuario();
        administrador.setNome("Administrador de teste");
        administrador.setEmail("admin-" + identificador + "@example.test");
        administrador.setSenhaHash("hash-ficticio-para-teste");
        administrador.setPerfil(PerfilUsuario.ADMIN);
        entityManager.persist(administrador);

        entityManager.flush();
        entityManager.clear();

        return new DadosConsulta(usuario.getId(), categoria.getId(), primeira.id(),
                segunda.id(), deOutroUsuario.id(), deOutraCategoria.id(), usuario.getEmail(), administrador.getEmail(), outroUsuario.getId());
    }

    private record DadosConsulta(Long usuarioId, Long categoriaId,
                                 Long primeiraId, Long segundaId,
                                 Long deOutroUsuarioId, Long deOutraCategoriaId, String usuarioEmail, String adminEmail, Long outroUsuarioId) {
    }
    @AfterEach
    void limparAutenticacao() {
        AutenticacaoTeste.limpar();
    }
}
