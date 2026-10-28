package io.github.joaogclima30.slinsight.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.server.ResponseStatusException;

@Configuration
public class PaginacaoConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ValidarPaginacaoInterceptor()).addPathPatterns("/ocorrencias/listar", "/ocorrencias/*/historico");
    }

    private static class ValidarPaginacaoInterceptor
            implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            validar(request, "page", 0, Integer.MAX_VALUE);
            validar(request, "size", 1, 100);
            return true;
        }

        private void validar(HttpServletRequest request, String parametro, int minimo, int maximo) {
            String[] valores = request.getParameterValues(parametro);

            if (valores == null) {
                return;
            }

            if (valores.length != 1) {
                throw invalido(parametro, minimo, maximo);
            }

            try {
                int valor = Integer.parseInt(valores[0]);

                if (valor < minimo || valor > maximo) {
                    throw invalido(parametro, minimo, maximo);
                }
            } catch (NumberFormatException exception) {
                throw invalido(parametro, minimo, maximo);
            }
        }

        private ResponseStatusException invalido(String parametro, int minimo, int maximo) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "O parâmetro '" + parametro
                    + "' deve ser um inteiro entre " + minimo + " e " + maximo);
        }
    }
}