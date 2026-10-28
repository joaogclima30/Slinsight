package io.github.joaogclima30.slinsight.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UsuarioRepository usuarioRepository
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/csrf")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/usuarios")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/categorias", "/categorias/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/categorias", "/categorias/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/categorias", "/categorias/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/categorias", "/categorias/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/ocorrencias/*/status")
                        .hasRole("ADMIN")

                        .anyRequest()
                        .authenticated());

        http.httpBasic(Customizer.withDefaults());
        http.formLogin(form -> form
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("senha")
                .successHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .failureHandler((request, response, exception) ->
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)).permitAll());
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        http.logout(logout -> logout
                .logoutUrl("/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)).permitAll());

        http.addFilterBefore(
                new AtualizarPerfilFilter(usuarioRepository),
                AuthorizationFilter.class
        );
        return http.build();
    }
}