package io.github.joaogclima30.slinsight.usuario.service;

import io.github.joaogclima30.slinsight.usuario.dto.UsuarioAutenticadoResponse;
import io.github.joaogclima30.slinsight.usuario.dto.UsuarioRequest;
import io.github.joaogclima30.slinsight.usuario.dto.UsuarioResponse;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;
import io.github.joaogclima30.slinsight.usuario.model.Usuario;
import io.github.joaogclima30.slinsight.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.joaogclima30.slinsight.usuario.model.PerfilUsuario;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        String email = request.email().strip();

        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um usuário com esse e-mail"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().strip());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(PerfilUsuario.CIDADAO);

        Usuario salvo = repository.save(usuario);

        return new UsuarioResponse(
                salvo.getId(),
                salvo.getNome(),
                salvo.getEmail(),
                salvo.getAtivo(),
                salvo.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        var autenticado = usuarioAutenticadoService.buscar();

        if (autenticado.getPerfil() != PerfilUsuario.ADMIN
                && !autenticado.getId().equals(id)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você só pode consultar seu próprio cadastro");
        }

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"));

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getAtivo(),
                usuario.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticadoResponse buscarMeuCadastro() {
        var usuario = usuarioAutenticadoService.buscar();

        return new UsuarioAutenticadoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil());
    }
}