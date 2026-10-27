package io.github.joaogclima30.slinsight.historico.controller;

import io.github.joaogclima30.slinsight.common.dto.PaginaResponse;
import io.github.joaogclima30.slinsight.historico.dto.HistoricoStatusResponse;
import io.github.joaogclima30.slinsight.historico.service.HistoricoStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocorrencias/{ocorrenciaId}/historico")
@RequiredArgsConstructor
public class HistoricoStatusController {

    private final HistoricoStatusService service;

    @GetMapping
    public PaginaResponse<HistoricoStatusResponse> listar(
            @PathVariable Long ocorrenciaId,
            @PageableDefault(
                    size = 10,
                    sort = {"alteradoEm", "id"},
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return PaginaResponse.de(
                service.listarPorOcorrencia(ocorrenciaId, pageable));
    }
}