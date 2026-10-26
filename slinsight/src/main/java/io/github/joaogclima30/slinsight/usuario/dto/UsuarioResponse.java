package io.github.joaogclima30.slinsight.usuario.dto;

import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Boolean ativo,
        Instant criadoEm
) {
}