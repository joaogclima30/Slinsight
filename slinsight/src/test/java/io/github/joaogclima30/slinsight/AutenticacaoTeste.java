package io.github.joaogclima30.slinsight;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

final class AutenticacaoTeste {

    private AutenticacaoTeste() {
    }

    static void autenticar(String email, String perfil) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
        SecurityContextHolder.setContext(contexto);
    }

    static void limpar() {
        SecurityContextHolder.clearContext();
    }

    static <T> T executarComo(String email, String perfil, Supplier<T> operacao) {
        autenticar(email, perfil);
        try {
            return operacao.get();
        } finally {
            limpar();
        }
    }
}
