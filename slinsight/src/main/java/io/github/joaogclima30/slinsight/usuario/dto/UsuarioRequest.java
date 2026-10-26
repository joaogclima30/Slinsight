package io.github.joaogclima30.slinsight.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter até 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 254, message = "O e-mail deve ter até 254 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 12, max = 64,
                message = "A senha deve ter entre 12 e 64 caracteres")
        String senha

) {
}