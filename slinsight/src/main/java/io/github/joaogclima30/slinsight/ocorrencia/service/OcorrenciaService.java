package io.github.joaogclima30.slinsight.ocorrencia.service;

import io.github.joaogclima30.slinsight.categoria.repository.CategoriaRepository;
import io.github.joaogclima30.slinsight.historico.model.HistoricoStatus;
import io.github.joaogclima30.slinsight.historico.repository.HistoricoStatusRepository;
import io.github.joaogclima30.slinsight.localizacao.repository.LocalizacaoRepository;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaRequest;
import io.github.joaogclima30.slinsight.ocorrencia.dto.OcorrenciaResponse;
import io.github.joaogclima30.slinsight.ocorrencia.model.Ocorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.model.StatusOcorrencia;
import io.github.joaogclima30.slinsight.ocorrencia.repository.OcorrenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.joaogclima30.slinsight.ocorrencia.dto.AlterarStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.github.joaogclima30.slinsight.ocorrencia.specification.OcorrenciaSpecification;
import io.github.joaogclima30.slinsight.usuario.service.UsuarioAutenticadoService;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OcorrenciaService {

    private final OcorrenciaRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public OcorrenciaResponse criar(OcorrenciaRequest request) {
        var usuario = usuarioAutenticadoService.buscar();

        var categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Categoria não encontrada"));

        var localizacao = localizacaoRepository.findById(request.localizacaoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Localização não encontrada"));


        if (!Boolean.TRUE.equals(categoria.getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A categoria está inativa");
        }

        Ocorrencia ocorrencia = new Ocorrencia(
                null,
                request.titulo().strip(),
                request.descricao().strip(),
                usuario,
                categoria,
                localizacao,
                StatusOcorrencia.ABERTA,
                null,
                null);

        Ocorrencia salva = repository.save(ocorrencia);

        HistoricoStatus historico = new HistoricoStatus(
                null,
                salva,
                usuario,
                null,
                StatusOcorrencia.ABERTA,
                "Ocorrência criada",
                null);

        historicoRepository.save(historico);

        return paraResponse(salva);
    }

    @Transactional
    public OcorrenciaResponse alterarStatus(Long id, AlterarStatusRequest request) {
        Ocorrencia ocorrencia = repository.buscarPorIdParaAtualizar(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ocorrência não encontrada"));

        var usuario = usuarioAutenticadoService.buscar();

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O usuário responsável está inativo");
        }

        StatusOcorrencia statusAnterior = ocorrencia.getStatus();

        if (!statusAnterior.permiteTransicaoPara(request.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é permitido alterar o status de "
                            + statusAnterior + " para " + request.status());
        }

        ocorrencia.setStatus(request.status());

        Ocorrencia salva = repository.saveAndFlush(ocorrencia);

        HistoricoStatus historico = new HistoricoStatus(
                null,
                salva,
                usuario,
                statusAnterior,
                request.status(),
                request.observacao(),
                null);

        historicoRepository.save(historico);

        return paraResponse(salva);
    }

    public OcorrenciaResponse buscarPorId(Long id) {
        Ocorrencia ocorrencia = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ocorrência não encontrada"));

        validarPermissaoDeConsulta(ocorrencia);

        return paraResponse(ocorrencia);
    }

    private OcorrenciaResponse paraResponse(Ocorrencia ocorrencia) {
        return new OcorrenciaResponse(
                ocorrencia.getId(),
                ocorrencia.getTitulo(),
                ocorrencia.getDescricao(),
                ocorrencia.getUsuario().getId(),
                ocorrencia.getCategoria().getId(),
                ocorrencia.getLocalizacao().getId(),
                ocorrencia.getStatus(),
                ocorrencia.getCriadoEm(),
                ocorrencia.getAtualizadoEm());
    }

    public Page<OcorrenciaResponse> listar(Long usuarioId, Long categoriaId, StatusOcorrencia status, Pageable pageable) {
        if (usuarioId != null && usuarioId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O ID do usuário deve ser positivo"
            );
        }

        if (categoriaId != null && categoriaId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O ID da categoria deve ser positivo");
        }

        var usuarioAutenticado = usuarioAutenticadoService.buscar();

        if (usuarioAutenticado.getPerfil() != PerfilUsuario.ADMIN) {
            if (usuarioId != null
                    && !usuarioId.equals(usuarioAutenticado.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Você só pode consultar suas próprias ocorrências");
            }
            usuarioId = usuarioAutenticado.getId();
        }

        var filtros = OcorrenciaSpecification.comFiltros(usuarioId, categoriaId, status);

        return repository.findAll(filtros, pageable)
                .map(this::paraResponse);
    }

    private void validarPermissaoDeConsulta(Ocorrencia ocorrencia) {
        var usuario = usuarioAutenticadoService.buscar();

        if (usuario.getPerfil() != PerfilUsuario.ADMIN
                && !ocorrencia.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem permissão para consultar esta ocorrência"
            );
        }
    }
}