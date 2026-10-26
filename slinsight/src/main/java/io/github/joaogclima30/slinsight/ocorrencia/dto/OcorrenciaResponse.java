package io.github.joaogclima30.slinsight.ocorrencia.dto;

import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;

import java.time.Instant;

public record OcorrenciaResponse(
        Long id,
        String titulo,
        String descricao,
        Long usuarioId,
        Long categoriaId,
        Long localizacaoId,
        StatusOcorrencia status,
        Instant criadoEm,
        Instant atualizadoEm
) {
}