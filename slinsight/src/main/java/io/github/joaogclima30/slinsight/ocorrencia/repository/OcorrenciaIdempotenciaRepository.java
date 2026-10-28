package io.github.joaogclima30.slinsight.ocorrencia.repository;

import io.github.joaogclima30.slinsight.ocorrencia.model.OcorrenciaIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OcorrenciaIdempotenciaRepository
        extends JpaRepository<OcorrenciaIdempotencia, Long> {

    Optional<OcorrenciaIdempotencia> findByUsuarioIdAndChave(Long usuarioId, String chave);
}