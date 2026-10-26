package io.github.joaogclima30.slinsight.ocorrencia.model;

public enum StatusOcorrencia {
    ABERTA,
    EM_ANALISE,
    EM_ANDAMENTO,
    RESOLVIDA,
    CANCELADA;

    public boolean permiteTransicaoPara(StatusOcorrencia destino) {
        if (destino == null) {
            return false;
        }

        return switch (this) {
            case ABERTA ->
                    destino == EM_ANALISE || destino == CANCELADA;

            case EM_ANALISE ->
                    destino == EM_ANDAMENTO || destino == CANCELADA;

            case EM_ANDAMENTO ->
                    destino == RESOLVIDA || destino == CANCELADA;

            case RESOLVIDA, CANCELADA -> false;
        };
    }
}