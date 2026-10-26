package io.github.joaogclima30.slinsight.usuario.controller;

import io.github.joaogclima30.slinsight.usuario.dto.UsuarioRequest;
import io.github.joaogclima30.slinsight.usuario.dto.UsuarioResponse;
import io.github.joaogclima30.slinsight.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(
            @Valid @RequestBody UsuarioRequest request
    ) {
        UsuarioResponse response = service.criar(request);

        return ResponseEntity
                .created(URI.create("/usuarios/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

}