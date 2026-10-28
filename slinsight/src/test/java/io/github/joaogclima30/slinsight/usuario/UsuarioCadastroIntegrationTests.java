package io.github.joaogclima30.slinsight.usuario;

import io.github.joaogclima30.slinsight.usuario.dto.UsuarioRequest;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import io.github.joaogclima30.slinsight.usuario.service.UsuarioService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UsuarioCadastroIntegrationTests {

    @Autowired
    private UsuarioService service;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveCadastrarCidadaoComSenhaProtegida() {
        String senha = "SenhaDeTeste123!";
        String email = UUID.randomUUID() + "@example.test";

        var resposta = service.criar(new UsuarioRequest(
                "Usuário de teste",
                email,
                senha
        ));

        entityManager.flush();
        entityManager.clear();

        var usuario = repository.findById(resposta.id())
                .orElseThrow();

        assertEquals(PerfilUsuario.CIDADAO, usuario.getPerfil());
        assertEquals(email, usuario.getEmail());
        assertTrue(usuario.getAtivo());
        assertNotNull(usuario.getCriadoEm());

        assertNotEquals(senha, usuario.getSenhaHash());
        assertTrue(passwordEncoder.matches(
                senha,
                usuario.getSenhaHash()
        ));
        assertFalse(passwordEncoder.matches(
                "SenhaIncorreta123!",
                usuario.getSenhaHash()
        ));
    }
}