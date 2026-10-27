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
import org.junit.jupiter.api.AfterEach;
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
        AutenticacaoTeste.autenticar(usuario.getEmail(), usuario.getPerfil().name());

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
                categoria.getId(),
                localizacao.getId()
        ));

        assertEquals(StatusOcorrencia.ABERTA, criada.status());

        var atualizada = service.alterarStatus(
                criada.id(),
                new AlterarStatusRequest(
                        StatusOcorrencia.EM_ANALISE,
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
        AutenticacaoTeste.autenticar(usuario.getEmail(), usuario.getPerfil().name());

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
        AutenticacaoTeste.autenticar(usuario.getEmail(), usuario.getPerfil().name());

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
                categoria.getId(),
                localizacao.getId()));

        // Para resolver, percorremos as etapas obrigatórias.
        if (statusFinal == StatusOcorrencia.RESOLVIDA) {
            service.alterarStatus(criada.id(), new AlterarStatusRequest(
                    StatusOcorrencia.EM_ANALISE,
                    "Em análise"));

            service.alterarStatus(criada.id(), new AlterarStatusRequest(
                    StatusOcorrencia.EM_ANDAMENTO,
                    "Em atendimento"));
        }

        service.alterarStatus(criada.id(), new AlterarStatusRequest(
                statusFinal,
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
    @AfterEach
    void limparAutenticacao() {
        AutenticacaoTeste.limpar();
    }

    @Test
    void deveRegistrarUsuarioAutenticadoComoResponsavelPelaAlteracao() {
        String identificador = UUID.randomUUID().toString();

        Usuario autor = new Usuario();
        autor.setNome("Autor da ocorrência");
        autor.setEmail("autor-" + identificador + "@example.test");
        autor.setSenhaHash("hash-ficticio-para-teste");
        entityManager.persist(autor);
        AutenticacaoTeste.autenticar(autor.getEmail(), autor.getPerfil().name());

        Usuario administrador = new Usuario();
        administrador.setNome("Administrador");
        administrador.setEmail("admin-" + identificador + "@example.test");
        administrador.setSenhaHash("hash-ficticio-para-teste");
        administrador.setPerfil(
                io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario.ADMIN
        );
        entityManager.persist(administrador);

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
                categoria.getId(),
                localizacao.getId()
        ));

        AutenticacaoTeste.autenticar(
                administrador.getEmail(),
                administrador.getPerfil().name()
        );

        service.alterarStatus(
                criada.id(),
                new AlterarStatusRequest(
                        StatusOcorrencia.EM_ANALISE,
                        "Análise iniciada pelo administrador"
                )
        );

        entityManager.flush();
        entityManager.clear();

        var ocorrencia = service.buscarPorId(criada.id());

        // O autor original permanece.
        assertEquals(autor.getId(), ocorrencia.usuarioId());

        var historico = historicoRepository.findByOcorrencia_Id(
                criada.id(),
                PageRequest.of(
                        0,
                        10,
                        Sort.by("alteradoEm", "id").ascending()
                )
        );

        assertEquals(2L, historico.getTotalElements());

        // A criação pertence ao autor.
        assertEquals(
                autor.getId(),
                historico.getContent().get(0).getUsuario().getId()
        );

        // A mudança pertence ao administrador autenticado.
        assertEquals(
                administrador.getId(),
                historico.getContent().get(1).getUsuario().getId()
        );
    }
}
