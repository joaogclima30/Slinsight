package io.github.joaogclima30.slinsight.historico.repository;

import io.github.joaogclima30.slinsight.historico.model.HistoricoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    Page<HistoricoStatus> findByOcorrencia_Id(Long ocorrenciaId, Pageable pageable);
}
