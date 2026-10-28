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
import io.github.joaogclima30.slinsight.ocorrencia.model.OcorrenciaIdempotencia;
import io.github.joaogclima30.slinsight.ocorrencia.repository.OcorrenciaIdempotenciaRepository;
import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OcorrenciaService {

    private final OcorrenciaRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final UsuarioRepository usuarioRepository;
    private final OcorrenciaIdempotenciaRepository idempotenciaRepository;


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

        boolean administrador = usuario.getPerfil() == PerfilUsuario.ADMIN;

        boolean autorDaLocalizacao = localizacao.getAutor() != null && localizacao.getAutor().getId().equals(usuario.getId());

        if (!administrador && !autorDaLocalizacao) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para usar esta localização");
        }

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
    public OcorrenciaResponse criar(OcorrenciaRequest request, String chave) {
        if (chave == null) {
            return criar(request);
        }

        if (chave.isBlank() || chave.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chave de idempotência deve ter entre 1 e 100 caracteres");
        }

        var autenticado = usuarioAutenticadoService.buscar();

        var usuario = usuarioRepository
                .buscarPorIdParaAtualizar(autenticado.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário não encontrado"));

        String assinatura = calcularAssinatura(request);

        var registroExistente = idempotenciaRepository.findByUsuarioIdAndChave(usuario.getId(), chave);

        if (registroExistente.isPresent()) {
            var registro = registroExistente.get();

            if (!registro.getAssinatura().equals(assinatura)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta chave de idempotência já foi usada com outros dados");
            }
            return buscarPorId(registro.getOcorrenciaId());
        }

        OcorrenciaResponse criada = criar(request);

        var registro = new OcorrenciaIdempotencia();
        registro.setUsuarioId(usuario.getId());
        registro.setChave(chave);
        registro.setAssinatura(assinatura);
        registro.setOcorrenciaId(criada.id());

        idempotenciaRepository.saveAndFlush(registro);

        return criada;
    }

    private String calcularAssinatura(OcorrenciaRequest request) {
        String titulo = request.titulo().strip();
        String descricao = request.descricao().strip();

        // Os comprimentos delimitam os textos sem ambiguidade.
        String conteudo =
                titulo.length() + ":" + titulo
                        + descricao.length() + ":" + descricao
                        + ":" + request.categoriaId()
                        + ":" + request.localizacaoId();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    conteudo.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível calcular a assinatura da requisição", exception);
        }
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