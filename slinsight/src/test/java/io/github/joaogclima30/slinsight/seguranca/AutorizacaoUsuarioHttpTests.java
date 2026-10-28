package io.github.joaogclima30.slinsight.seguranca;

import io.github.joaogclima30.slinsight.suporte.SegurancaHttpTestSupport;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutorizacaoUsuarioHttpTests extends SegurancaHttpTestSupport {

    @Test
    @Transactional
    void devePermitirCidadaoConsultarProprioCadastro() throws Exception {

        String email = UUID.randomUUID() + "@example.test";
        String senha = "SenhaDeTeste123!";

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(PerfilUsuario.CIDADAO);

        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/usuarios/{id}", usuario.getId())
                        .with(httpBasic(email, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        usuario.getId().longValue()))
                .andExpect(jsonPath("$.nome").value("Usuário de teste"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    @Transactional
    void deveRetornar403AoConsultarCadastroDeOutroUsuario() throws Exception {

        String identificador = UUID.randomUUID().toString();
        String email = "cidadao-" + identificador + "@example.test";
        String senha = "SenhaDeTeste123!";

        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadão");
        cidadao.setEmail(email);
        cidadao.setSenhaHash(passwordEncoder.encode(senha));
        cidadao.setPerfil(PerfilUsuario.CIDADAO);
        entityManager.persist(cidadao);

        Usuario outroUsuario = new Usuario();
        outroUsuario.setNome("Outro usuário");
        outroUsuario.setEmail("outro-" + identificador + "@example.test");
        outroUsuario.setSenhaHash(passwordEncoder.encode(senha));
        outroUsuario.setPerfil(PerfilUsuario.CIDADAO);
        entityManager.persist(outroUsuario);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/usuarios/{id}", outroUsuario.getId())
                        .with(httpBasic(email, senha)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(
                        "Você só pode consultar seu próprio cadastro"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    @Transactional
    void devePermitirAdministradorConsultarOutroUsuario()
            throws Exception {

        String identificador = UUID.randomUUID().toString();
        String emailAdmin = "admin-" + identificador + "@example.test";
        String senha = "SenhaDeTeste123!";

        Usuario administrador = new Usuario();
        administrador.setNome("Administrador");
        administrador.setEmail(emailAdmin);
        administrador.setSenhaHash(passwordEncoder.encode(senha));
        administrador.setPerfil(PerfilUsuario.ADMIN);
        entityManager.persist(administrador);

        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadão de teste");
        cidadao.setEmail("cidadao-" + identificador + "@example.test");
        cidadao.setSenhaHash(passwordEncoder.encode(senha));
        cidadao.setPerfil(PerfilUsuario.CIDADAO);
        entityManager.persist(cidadao);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/usuarios/{id}", cidadao.getId())
                        .with(httpBasic(emailAdmin, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        cidadao.getId().longValue()
                ))
                .andExpect(jsonPath("$.nome").value("Cidadão de teste"))
                .andExpect(jsonPath("$.email").value(cidadao.getEmail()))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    @Transactional
    void deveRetornar404QuandoAdministradorConsultaUsuarioInexistente()
            throws Exception {

        String email = "admin-" + UUID.randomUUID() + "@example.test";
        String senha = "SenhaDeTeste123!";

        Usuario administrador = new Usuario();
        administrador.setNome("Administrador");
        administrador.setEmail(email);
        administrador.setSenhaHash(passwordEncoder.encode(senha));
        administrador.setPerfil(PerfilUsuario.ADMIN);

        entityManager.persist(administrador);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/usuarios/{id}", Long.MAX_VALUE)
                        .with(httpBasic(email, senha)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Usuário não encontrado"));
    }

    @Test
    void deveRetornar401AoConsultarUsuarioSemAutenticacao()
            throws Exception {

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void deveRetornarCadastroDoUsuarioAutenticado()
            throws Exception {

        String email = UUID.randomUUID() + "@example.test";
        String senha = "SenhaDeTeste123!";

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário conectado");
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(PerfilUsuario.CIDADAO);

        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/usuarios/me")
                        .with(httpBasic(email, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        usuario.getId().longValue()))
                .andExpect(jsonPath("$.nome").value("Usuário conectado"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.perfil").value("CIDADAO"));
    }

    @Test
    void deveRetornar401AoConsultarMeuCadastroSemAutenticacao()
            throws Exception {

        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }
}
