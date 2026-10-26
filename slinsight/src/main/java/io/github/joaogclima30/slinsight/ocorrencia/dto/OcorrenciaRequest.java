package io.github.joaogclima30.slinsight.ocorrencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OcorrenciaRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 200, message = "O título deve ter até 200 caracteres")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotNull(message = "O usuário é obrigatório")
        @Positive(message = "O ID do usuário deve ser positivo")
        Long usuarioId,

        @NotNull(message = "A categoria é obrigatória")
        @Positive(message = "O ID da categoria deve ser positivo")
        Long categoriaId,

        @NotNull(message = "A localização é obrigatória")
        @Positive(message = "O ID da localização deve ser positivo")
        Long localizacaoId

) {
}