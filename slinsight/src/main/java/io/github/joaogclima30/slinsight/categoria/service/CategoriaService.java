package io.github.joaogclima30.slinsight.categoria.service;

import io.github.joaogclima30.slinsight.categoria.dto.CategoriaRequest;
import io.github.joaogclima30.slinsight.categoria.dto.CategoriaResponse;
import io.github.joaogclima30.slinsight.categoria.model.Categoria;
import io.github.joaogclima30.slinsight.categoria.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository repository;

    public List<CategoriaResponse> listar() {
        return repository.findAll()
                .stream()
                .map(this::paraResponse)
                .toList();
    }

    public CategoriaResponse buscarPorId(Long id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        String nome = request.nome().strip();

        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma categoria com esse nome"
            );
        }

        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setDescricao(request.descricao());

        return paraResponse(repository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidade(id);
        String nome = request.nome().strip();

        repository.findByNomeIgnoreCase(nome)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe uma categoria com esse nome"
                    );
                });

        categoria.setNome(nome);
        categoria.setDescricao(request.descricao());

        return paraResponse(repository.save(categoria));
    }

    @Transactional
    public void desativar(Long id) {
        Categoria categoria = buscarEntidade(id);
        categoria.setAtivo(false);
        repository.save(categoria);
    }

    private Categoria buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Categoria não encontrada"
                ));
    }

    private CategoriaResponse paraResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getAtivo()
        );
    }
}