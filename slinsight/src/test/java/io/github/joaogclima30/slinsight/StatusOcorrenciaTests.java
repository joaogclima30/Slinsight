package io.github.joaogclima30.slinsight;

import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StatusOcorrenciaTests {

    @Test
    void devePermitirApenasAsTransicoesDefinidas() {
        Map<StatusOcorrencia, Set<StatusOcorrencia>> permitidas = Map.of(
                StatusOcorrencia.ABERTA,
                Set.of(
                        StatusOcorrencia.EM_ANALISE,
                        StatusOcorrencia.CANCELADA),
                StatusOcorrencia.EM_ANALISE,
                Set.of(
                        StatusOcorrencia.EM_ANDAMENTO,
                        StatusOcorrencia.CANCELADA),
                StatusOcorrencia.EM_ANDAMENTO,
                Set.of(
                        StatusOcorrencia.RESOLVIDA,
                        StatusOcorrencia.CANCELADA),
                StatusOcorrencia.RESOLVIDA, Set.of(),
                StatusOcorrencia.CANCELADA, Set.of());

        for (StatusOcorrencia origem : StatusOcorrencia.values()) {
            for (StatusOcorrencia destino : StatusOcorrencia.values()) {
                assertEquals(
                        permitidas.get(origem).contains(destino),
                        origem.permiteTransicaoPara(destino),
                        () -> "Transição: " + origem + " → " + destino);
            }
        }
    }

    @Test
    void deveRejeitarDestinoNulo() {
        for (StatusOcorrencia origem : StatusOcorrencia.values()) {
            assertFalse(
                    origem.permiteTransicaoPara(null),
                    () -> "Destino nulo deve ser rejeitado para " + origem);
        }
    }
}