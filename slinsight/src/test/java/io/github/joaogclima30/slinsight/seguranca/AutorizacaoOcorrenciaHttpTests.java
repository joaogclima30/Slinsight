package io.github.joaogclima30.slinsight.seguranca;

import io.github.joaogclima30.slinsight.suporte.SegurancaHttpTestSupport;
import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.ocorrencia.model.Ocorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutorizacaoOcorrenciaHttpTests extends SegurancaHttpTestSupport {

    @Test
    void deveRetornar401AoListarOcorrenciasSemAutenticacao()
            throws Exception {

        mockMvc.perform(get("/ocorrencias/listar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar403QuandoCidadaoTentaAlterarStatus() throws Exception {
        mockMvc.perform(patch("/ocorrencias/1/status")
                        .with(user("cidadao@example.test").roles("CIDADAO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "EM_ANALISE",
                              "observacao": "Tentativa sem permissão"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar403QuandoAdministradorNaoEnviaCsrf()
            throws Exception {

        mockMvc.perform(patch("/ocorrencias/1/status")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "EM_ANALISE",
                              "observacao": "Tentativa sem token CSRF"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAdministradorComCsrfAlterarStatus() throws Exception {

        String identificador = UUID.randomUUID().toString();

        Usuario administrador = new Usuario();
        administrador.setNome("Administrador de teste");
        administrador.setEmail("admin-" + identificador + "@example.test");
        administrador.setSenhaHash("hash-ficticio-para-teste");
        administrador.setPerfil(PerfilUsuario.ADMIN);
        entityManager.persist(administrador);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + identificador);
        entityManager.persist(categoria);

        Localizacao localizacao = new Localizacao();
        localizacao.setCidade("São Paulo");
        localizacao.setUf("SP");
        entityManager.persist(localizacao);

        Ocorrencia ocorrencia = new Ocorrencia(
                null,
                "Poste apagado",
                "Iluminação apagada há três dias",
                administrador,
                categoria,
                localizacao,
                StatusOcorrencia.ABERTA,
                null,
                null);

        entityManager.persist(ocorrencia);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(patch(
                        "/ocorrencias/{id}/status",
                        ocorrencia.getId())
                        .with(user(administrador.getEmail()).roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "EM_ANALISE",
                              "observacao": "Análise iniciada pelo administrador"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        ocorrencia.getId().longValue())).andExpect(jsonPath("$.status").value("EM_ANALISE"));

        entityManager.flush();
        entityManager.clear();

        var salva = entityManager.find(
                Ocorrencia.class,
                ocorrencia.getId());

        assertEquals(StatusOcorrencia.EM_ANALISE, salva.getStatus());
    }
}
