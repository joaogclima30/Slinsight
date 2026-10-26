package io.github.joaogclima30.slinsight.ocorrencia.dto;

import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AlterarStatusRequest(

        @NotNull(message = "O novo status é obrigatório")
        StatusOcorrencia status,

        @NotNull(message = "O usuário responsável é obrigatório")
        @Positive(message = "O ID do usuário deve ser positivo")
        Long usuarioId,

        @Size(
                max = 2000,
                message = "A observação deve ter até 2000 caracteres"
        )
        String observacao

) {
}