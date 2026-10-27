package io.github.joaogclima30.slinsight.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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

        return http.build();
    }
}