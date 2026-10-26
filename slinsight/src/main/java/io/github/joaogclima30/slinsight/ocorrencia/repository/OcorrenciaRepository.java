package io.github.joaogclima30.slinsight.ocorrencia.repository;

import io.github.joaogclima30.slinsight.ocorrencia.model.Ocorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OcorrenciaRepository
        extends JpaRepository<Ocorrencia, Long>,
        JpaSpecificationExecutor<Ocorrencia> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Ocorrencia o where o.id = :id")
    Optional<Ocorrencia> buscarPorIdParaAtualizar(@Param("id") Long id);

    Page<Ocorrencia> findByUsuario_Id(Long usuarioId, Pageable pageable);

    Page<Ocorrencia> findByCategoria_Id(Long categoriaId, Pageable pageable);

    Page<Ocorrencia> findByStatus(StatusOcorrencia status, Pageable pageable);
}