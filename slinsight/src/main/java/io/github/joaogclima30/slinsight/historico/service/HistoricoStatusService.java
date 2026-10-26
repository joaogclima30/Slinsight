package io.github.joaogclima30.slinsight.historico.service;

import io.github.joaogclima30.slinsight.historico.dto.HistoricoStatusResponse;
import io.github.joaogclima30.slinsight.historico.model.HistoricoStatus;
import io.github.joaogclima30.slinsight.historico.repository.HistoricoStatusRepository;
import io.github.joaogclima30.slinsight.ocorrencia.repository.OcorrenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoricoStatusService {

    private final HistoricoStatusRepository repository;
    private final OcorrenciaRepository ocorrenciaRepository;

    public Page<HistoricoStatusResponse> listarPorOcorrencia(Long ocorrenciaId, Pageable pageable) {
        if (!ocorrenciaRepository.existsById(ocorrenciaId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Ocorrência não encontrada"
            );
        }

        return repository
                .findByOcorrencia_Id(ocorrenciaId, pageable)
                .map(this::paraResponse);
    }

    private HistoricoStatusResponse paraResponse(HistoricoStatus historico) {
        return new HistoricoStatusResponse(
                historico.getId(),
                historico.getOcorrencia().getId(),
                historico.getUsuario().getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getObservacao(),
                historico.getAlteradoEm()
        );
    }
}