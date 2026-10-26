package io.github.joaogclima30.slinsight.ocorrencia.controller;

import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaRequest;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaResponse;
import io.github.joaogclima30.slinsight.ocorrencia.service.OcorrenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import io.github.joaogclima30.slinsight.ocorrencia.dto.AlterarStatusRequest;

import java.net.URI;

@RestController
@RequestMapping("/ocorrencias")
@RequiredArgsConstructor
public class OcorrenciaController {

    private final OcorrenciaService service;

    @PostMapping("/criar")
    public ResponseEntity<OcorrenciaResponse> criar(@Valid @RequestBody OcorrenciaRequest request) {
        OcorrenciaResponse response = service.criar(request);

        return ResponseEntity
                .created(URI.create("/ocorrencias/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public OcorrenciaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/status")
    public OcorrenciaResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusRequest request) {
        return service.alterarStatus(id, request);
    }

    @GetMapping("/listar")
    public Page<OcorrenciaResponse> listar(@RequestParam(required = false) Long usuarioId, @RequestParam(required = false) Long categoriaId,
                                           @RequestParam(required = false) StatusOcorrencia status,
                                           @PageableDefault(size = 10, sort = {"criadoEm", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(
                usuarioId,
                categoriaId,
                status,
                pageable);
    }
}