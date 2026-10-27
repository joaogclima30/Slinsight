package io.github.joaogclima30.slinsight.config;

import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        var usuario = repository.findByEmailIgnoreCase(email.strip())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Credenciais inválidas"));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPerfil().name())
                .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                .build();
    }
}