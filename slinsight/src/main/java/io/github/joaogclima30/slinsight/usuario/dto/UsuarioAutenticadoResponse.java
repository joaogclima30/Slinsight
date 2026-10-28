package io.github.joaogclima30.slinsight.usuario.dto;

import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;

public record UsuarioAutenticadoResponse(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil) {
}