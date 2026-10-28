package io.github.joaogclima30.slinsight.seguranca;

import io.github.joaogclima30.slinsight.suporte.SegurancaHttpTestSupport;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.web.csrf.CsrfToken;

class AutenticacaoSessaoHttpTests extends SegurancaHttpTestSupport {

    @Test
    @Transactional
    void deveAutenticarPorSessaoEEncerrarAcessoNoLogout()
            throws Exception {

        String senha = "SenhaDeTeste123!";

        Usuario usuario = criarUsuario(
                "Usuário da sessão",
                senha,
                PerfilUsuario.CIDADAO,
                true);

        entityManager.clear();

        // Autentica pelo endpoint de login, sem HTTP Basic.
        var login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", usuario.getEmail())
                        .param("senha", senha))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession)
                login.getRequest().getSession(false);

        assertNotNull(sessao);

        // Usa somente a sessão para consultar o usuário.
        mockMvc.perform(get("/usuarios/me")
                        .session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        usuario.getId().longValue()))
                .andExpect(jsonPath("$.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.perfil").value("CIDADAO"));

        // Encerra a sessão com CSRF válido.
        mockMvc.perform(post("/auth/logout")
                        .session(sessao)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));

        org.junit.jupiter.api.Assertions.assertTrue(sessao.isInvalid());

        // Sem a sessão e sem credenciais, o acesso é negado.
        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void deveRejeitarSenhaIncorretaNoLoginPorSessao()
            throws Exception {

        Usuario usuario = criarUsuario(
                "Usuário de teste",
                "SenhaCorreta123!",
                PerfilUsuario.CIDADAO,
                true
        );

        entityManager.clear();

        var resultado = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", usuario.getEmail())
                        .param("senha", "SenhaIncorreta123!"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession)
                resultado.getRequest().getSession(false);

        var consulta = get("/usuarios/me");

        if (sessao != null) {
            consulta.session(sessao);
        }

        mockMvc.perform(consulta)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void deveAutenticarComTokenObtidoPeloEndpointCsrf()
            throws Exception {

        String senha = "SenhaDeTeste123!";

        Usuario usuario = criarUsuario(
                "Usuário da sessão",
                senha,
                PerfilUsuario.CIDADAO,
                true
        );

        entityManager.clear();

        var consultaCsrf = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession)
                consultaCsrf.getRequest().getSession(false);

        CsrfToken token = (CsrfToken) consultaCsrf.getRequest()
                .getAttribute(CsrfToken.class.getName());

        assertNotNull(sessao);
        assertNotNull(token);

        var login = mockMvc.perform(post("/auth/login")
                        .session(sessao)
                        .header(token.getHeaderName(), token.getToken())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", usuario.getEmail())
                        .param("senha", senha))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession sessaoAutenticada = (MockHttpSession)
                login.getRequest().getSession(false);

        assertNotNull(sessaoAutenticada);

        mockMvc.perform(get("/usuarios/me")
                        .session(sessaoAutenticada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));
    }

    @Test
    @Transactional
    void deveRejeitarLoginSemTokenCsrf() throws Exception {
        String senha = "SenhaDeTeste123!";

        Usuario usuario = criarUsuario(
                "Usuário de teste",
                senha,
                PerfilUsuario.CIDADAO,
                true
        );

        entityManager.clear();

        var resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", usuario.getEmail())
                        .param("senha", senha))
                .andExpect(status().isForbidden())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession)
                resultado.getRequest().getSession(false);

        var consulta = get("/usuarios/me");

        if (sessao != null) {
            consulta.session(sessao);
        }

        mockMvc.perform(consulta)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void deveExigirNovoTokenCsrfDepoisDoLogin() throws Exception {
        String senha = "SenhaDeTeste123!";

        Usuario usuario = criarUsuario(
                "Usuário da sessão",
                senha,
                PerfilUsuario.CIDADAO,
                true
        );

        entityManager.clear();

        var consultaInicial = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession)
                consultaInicial.getRequest().getSession(false);

        CsrfToken tokenAntigo = (CsrfToken) consultaInicial
                .getRequest()
                .getAttribute(CsrfToken.class.getName());

        assertNotNull(sessao);
        assertNotNull(tokenAntigo);

        var login = mockMvc.perform(post("/auth/login")
                        .session(sessao)
                        .header(
                                tokenAntigo.getHeaderName(),
                                tokenAntigo.getToken()
                        )
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", usuario.getEmail())
                        .param("senha", senha))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession sessaoAutenticada = (MockHttpSession)
                login.getRequest().getSession(false);

        assertNotNull(sessaoAutenticada);

        // O token anterior não pode ser reutilizado.
        mockMvc.perform(post("/auth/logout")
                        .session(sessaoAutenticada)
                        .header(
                                tokenAntigo.getHeaderName(),
                                tokenAntigo.getToken()
                        ))
                .andExpect(status().isForbidden());

        // A tentativa rejeitada não deve encerrar a sessão.
        mockMvc.perform(get("/usuarios/me")
                        .session(sessaoAutenticada))
                .andExpect(status().isOk());

        // Obtém um token válido para a sessão autenticada.
        var novaConsulta = mockMvc.perform(get("/csrf")
                        .session(sessaoAutenticada))
                .andExpect(status().isOk())
                .andReturn();

        CsrfToken tokenNovo = (CsrfToken) novaConsulta
                .getRequest()
                .getAttribute(CsrfToken.class.getName());

        assertNotNull(tokenNovo);

        mockMvc.perform(post("/auth/logout")
                        .session(sessaoAutenticada)
                        .header(tokenNovo.getHeaderName(), tokenNovo.getToken()))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(
                sessaoAutenticada.isInvalid()
        );
    }
}
