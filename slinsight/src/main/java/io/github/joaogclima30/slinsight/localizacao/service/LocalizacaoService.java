package io.github.joaogclima30.slinsight.localizacao.service;

import io.github.joaogclima30.slinsight.localizacao.dto.LocalizacaoRequest;
import io.github.joaogclima30.slinsight.localizacao.dto.LocalizacaoResponse;
import io.github.joaogclima30.slinsight.localizacao.model.Localizacao;
import io.github.joaogclima30.slinsight.localizacao.repository.LocalizacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalizacaoService {

    private final LocalizacaoRepository repository;

    @Transactional
    public LocalizacaoResponse criar(LocalizacaoRequest request) {
        Localizacao localizacao = new Localizacao();

        localizacao.setLogradouro(request.logradouro());
        localizacao.setNumero(request.numero());
        localizacao.setComplemento(request.complemento());
        localizacao.setBairro(request.bairro());
        localizacao.setCidade(request.cidade().strip());
        localizacao.setUf(request.uf());
        localizacao.setCep(request.cep());
        localizacao.setLatitude(request.latitude());
        localizacao.setLongitude(request.longitude());

        return paraResponse(repository.save(localizacao));
    }

    public LocalizacaoResponse buscarPorId(Long id) {
        Localizacao localizacao = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Localização não encontrada"));

        return paraResponse(localizacao);
    }

    private LocalizacaoResponse paraResponse(Localizacao localizacao) {
        return new LocalizacaoResponse(
                localizacao.getId(),
                localizacao.getLogradouro(),
                localizacao.getNumero(),
                localizacao.getComplemento(),
                localizacao.getBairro(),
                localizacao.getCidade(),
                localizacao.getUf(),
                localizacao.getCep(),
                localizacao.getLatitude(),
                localizacao.getLongitude());
    }
}