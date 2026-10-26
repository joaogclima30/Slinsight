package io.github.joaogclima30.slinsight.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(@NotBlank(message = "O nome é obrigatório")
                               @Size(max = 100, message = "O nome deve ter até 100 caracteres")
                               String nome,
                               String descricao) {
}