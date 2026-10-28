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

class AutenticacaoBasicHttpTests extends SegurancaHttpTestSupport {

    @Test
    @Transactional
    void deveAutenticarComEmailESenhaViaHttpBasic() throws Exception {

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

        mockMvc.perform(get("/ocorrencias/listar")
                        .with(httpBasic(email, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @Transactional
    void deveRetornar401QuandoSenhaEstiverIncorreta() throws Exception {

        String email = UUID.randomUUID() + "@example.test";

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(email);
        usuario.setSenhaHash(
                passwordEncoder.encode("SenhaCorreta123!"));
        usuario.setPerfil(PerfilUsuario.CIDADAO);

        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/ocorrencias/listar")
                        .with(httpBasic(email, "SenhaIncorreta123!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void deveRetornar401QuandoUsuarioEstiverInativo() throws Exception {

        String email = UUID.randomUUID() + "@example.test";
        String senha = "SenhaDeTeste123!";

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário inativo");
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(PerfilUsuario.CIDADAO);
        usuario.setAtivo(false);

        entityManager.persist(usuario);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/ocorrencias/listar")
                        .with(httpBasic(email, senha)))
                .andExpect(status().isUnauthorized());
    }
}
