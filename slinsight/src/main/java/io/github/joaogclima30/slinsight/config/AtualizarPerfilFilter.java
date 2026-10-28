package io.github.joaogclima30.slinsight.config;

import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class AtualizarPerfilFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public AtualizarPerfilFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {

            filterChain.doFilter(request, response);
            return;
        }

        var usuario = usuarioRepository.findByEmailIgnoreCase(autenticacao.getName()).orElse(null);

        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) {
            SecurityContextHolder.clearContext();
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Usuário indisponível ou inativo");
            return;
        }

        var permissoes = List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + usuario.getPerfil().name()));

        var autenticacaoAtualizada =
                new UsernamePasswordAuthenticationToken(
                        autenticacao.getPrincipal(),
                        autenticacao.getCredentials(),
                        permissoes);

        autenticacaoAtualizada.setDetails(autenticacao.getDetails());

        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacaoAtualizada);
        SecurityContextHolder.setContext(contexto);

        filterChain.doFilter(request, response);
    }
}