package io.github.joaogclima30.slinsight.categoria.dto;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        Boolean ativo) {
}