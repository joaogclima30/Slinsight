package io.github.joaogclima30.slinsight;

import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.historico.model.HistoricoStatus;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.ocorrencia.model.Ocorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EntidadesPersistenceTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void devePersistirERelerOcorrenciaComRelacionamentosEHistorico() {
        String identificador = UUID.randomUUID().toString();
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de teste");
        usuario.setEmail(identificador + "@example.test");
        usuario.setSenhaHash("hash-ficticio-apenas-para-teste");
        entityManager.persist(usuario);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("Sao Paulo");
        localizacao.setUf("SP");
        localizacao.setCep("01001000");
        localizacao.setLatitude(new BigDecimal("-23.550520"));
        localizacao.setLongitude(new BigDecimal("-46.633308"));
        entityManager.persist(localizacao);

        Ocorrencia ocorrencia = new Ocorrencia(null, "Ocorrencia de teste",
                "Descricao de teste", usuario, categoria, localizacao,
                StatusOcorrencia.ABERTA, null, null);
        entityManager.persist(ocorrencia);
        HistoricoStatus historico = new HistoricoStatus(null, ocorrencia, usuario,
                null, StatusOcorrencia.ABERTA, "Registro inicial", null);
        entityManager.persist(historico);
        entityManager.flush();
        Long ocorrenciaId = ocorrencia.getId();
        Long historicoId = historico.getId();
        entityManager.clear();

        Ocorrencia encontrada = entityManager.find(Ocorrencia.class, ocorrenciaId);
        assertEquals(usuario.getId(), encontrada.getUsuario().getId());
        assertTrue(encontrada.getUsuario().getAtivo());
        assertNotNull(encontrada.getUsuario().getCriadoEm());
        assertEquals("hash-ficticio-apenas-para-teste", encontrada.getUsuario().getSenhaHash());
        assertTrue(encontrada.getCategoria().getAtivo());
        assertEquals("SP", encontrada.getLocalizacao().getUf());
        assertEquals(new BigDecimal("-23.550520"), encontrada.getLocalizacao().getLatitude());
        assertEquals(StatusOcorrencia.ABERTA, encontrada.getStatus());
        assertNotNull(encontrada.getCriadoEm());
        assertNotNull(encontrada.getAtualizadoEm());

        HistoricoStatus registro = entityManager.find(HistoricoStatus.class, historicoId);
        assertEquals(ocorrenciaId, registro.getOcorrencia().getId());
        assertEquals(usuario.getId(), registro.getUsuario().getId());
        assertNull(registro.getStatusAnterior());
        assertEquals(StatusOcorrencia.ABERTA, registro.getStatusNovo());
        assertNotNull(registro.getAlteradoEm());
    }
}
