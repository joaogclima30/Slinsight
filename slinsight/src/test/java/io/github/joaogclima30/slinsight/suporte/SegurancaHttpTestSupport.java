package io.github.joaogclima30.slinsight.suporte;

import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
public abstract class SegurancaHttpTestSupport {

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    @PersistenceContext
    protected EntityManager entityManager;

    protected MockMvc mockMvc;

    @BeforeEach
    protected void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected Usuario criarUsuario(
            String nome,
            String senha,
            PerfilUsuario perfil,
            boolean ativo
    ) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(UUID.randomUUID() + "@example.test");
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }
}
