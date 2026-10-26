package io.github.joaogclima30.slinsight.localizacao.controller;

import io.github.joaogclima30.slinsight.localizacao.dto.LocalizacaoRequest;
import io.github.joaogclima30.slinsight.localizacao.dto.LocalizacaoResponse;
import io.github.joaogclima30.slinsight.localizacao.service.LocalizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/localizacoes")
@RequiredArgsConstructor
public class LocalizacaoController {

    private final LocalizacaoService service;

    @PostMapping
    public ResponseEntity<LocalizacaoResponse> criar(
            @Valid @RequestBody LocalizacaoRequest request
    ) {
        LocalizacaoResponse response = service.criar(request);

        return ResponseEntity
                .created(URI.create("/localizacoes/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public LocalizacaoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}