package io.github.joaogclima30.slinsight.suporte;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

public final class AutenticacaoTeste {

    private AutenticacaoTeste() {
    }

    public static void autenticar(String email, String perfil) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
        SecurityContextHolder.setContext(contexto);
    }

    public static void limpar() {
        SecurityContextHolder.clearContext();
    }

    public static <T> T executarComo(String email, String perfil, Supplier<T> operacao) {
        autenticar(email, perfil);
        try {
            return operacao.get();
        } finally {
            limpar();
        }
    }
}
