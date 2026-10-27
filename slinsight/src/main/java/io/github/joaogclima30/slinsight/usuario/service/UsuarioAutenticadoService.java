package io.github.joaogclima30.slinsight.usuario.service;

import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UsuarioRepository repository;

    @Transactional(readOnly = true)
    public Usuario buscar() {
        Authentication autenticacao = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Autenticação necessária");
        }

        Usuario usuario = repository
                .findByEmailIgnoreCase(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado"));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "O usuário está inativo");
        }

        return usuario;
    }
}