package io.github.joaogclima30.slinsight.historico.dto;

import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;

import java.time.Instant;

public record HistoricoStatusResponse(
        Long id,
        Long ocorrenciaId,
        Long usuarioId,
        StatusOcorrencia statusAnterior,
        StatusOcorrencia statusNovo,
        String observacao,
        Instant alteradoEm
) {
}