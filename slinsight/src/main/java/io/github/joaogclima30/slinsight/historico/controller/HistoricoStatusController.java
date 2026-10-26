package io.github.joaogclima30.slinsight.historico.controller;

import io.github.joaogclima30.slinsight.historico.dto.HistoricoStatusResponse;
import io.github.joaogclima30.slinsight.historico.service.HistoricoStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ocorrencias/{ocorrenciaId}/historico")
@RequiredArgsConstructor
public class HistoricoStatusController {

    private final HistoricoStatusService service;

    @GetMapping
    public Page<HistoricoStatusResponse> listar(
            @PathVariable Long ocorrenciaId,
            @PageableDefault(
                    size = 10,
                    sort = {"alteradoEm", "id"},
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return service.listarPorOcorrencia(ocorrenciaId, pageable);
    }
}