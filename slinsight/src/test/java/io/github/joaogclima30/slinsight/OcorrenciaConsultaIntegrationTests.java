package io.github.joaogclima30.slinsight;

import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.ocorrencia.dto.AlterarStatusRequest;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaRequest;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.service.OcorrenciaService;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveCombinarFiltrosPorUsuarioCategoriaEStatus() {
        var d = prepararDados();
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
        var pagina = service.listar(d.usuarioId(), null, null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutraCategoriaId()), ids(pagina));
    }

    @Test
    void deveFiltrarSomentePorCategoria() {
        var d = prepararDados();
        var pagina = service.listar(null, d.categoriaId(), null, PageRequest.of(0, 10));
        assertEquals(3L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.segundaId(), d.deOutroUsuarioId()), ids(pagina));
    }

    @Test
    void deveFiltrarPorUsuarioEStatus() {
        var d = prepararDados();
        var pagina = service.listar(d.usuarioId(), null,
                StatusOcorrencia.ABERTA, PageRequest.of(0, 10));
        assertEquals(2L, pagina.getTotalElements());
        assertEquals(Set.of(d.primeiraId(), d.deOutraCategoriaId()), ids(pagina));
    }

    @Test
    void devePaginarSemRepetirRegistros() {
        var d = prepararDados();
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

        Categoria outraCategoria = new Categoria();
        outraCategoria.setNome("Outra categoria " + UUID.randomUUID());
        entityManager.persist(outraCategoria);

        AutenticacaoTeste.autenticar(outroUsuario.getEmail(), outroUsuario.getPerfil().name());
        var deOutroUsuario = service.criar(new OcorrenciaRequest(
                "Ocorrência de outro usuário",
                "Não deve aparecer no filtro combinado",
                categoria.getId(),
                localizacao.getId()
        ));


        AutenticacaoTeste.autenticar(usuario.getEmail(), usuario.getPerfil().name());
        var deOutraCategoria = service.criar(new OcorrenciaRequest(
                "Ocorrência de outra categoria",
                "Não deve aparecer no filtro combinado",
                outraCategoria.getId(),
                localizacao.getId()
        ));

        entityManager.flush();
        entityManager.clear();

        return new DadosConsulta(usuario.getId(), categoria.getId(), primeira.id(),
                segunda.id(), deOutroUsuario.id(), deOutraCategoria.id());
    }

    private record DadosConsulta(Long usuarioId, Long categoriaId,
                                 Long primeiraId, Long segundaId,
                                 Long deOutroUsuarioId, Long deOutraCategoriaId) {
    }
    @AfterEach
    void limparAutenticacao() {
        AutenticacaoTeste.limpar();
    }
}
