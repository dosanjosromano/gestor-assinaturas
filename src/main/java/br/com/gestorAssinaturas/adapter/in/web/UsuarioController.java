package br.com.gestorAssinaturas.adapter.in.web;

import br.com.gestorAssinaturas.application.port.in.BuscaUsuariosUseCase;
import br.com.gestorAssinaturas.application.port.in.CriarUsuarioCommand;
import br.com.gestorAssinaturas.application.port.in.CriarUsuarioUseCase;
import br.com.gestorAssinaturas.domain.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final CriarUsuarioUseCase criarUsuarioUseCase;
    private final BuscaUsuariosUseCase buscaUsuariosUseCase;

    public UsuarioController(CriarUsuarioUseCase criarUsuarioUseCase, BuscaUsuariosUseCase buscaUsuariosUseCase) {
        this.criarUsuarioUseCase = criarUsuarioUseCase;
        this.buscaUsuariosUseCase = buscaUsuariosUseCase;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        Usuario usuario = criarUsuarioUseCase.criar(new CriarUsuarioCommand(request.nome(), request.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(usuario));
    }

    @GetMapping
    public List<UsuarioResponse> listarTodos() {
        return buscaUsuariosUseCase.listarTodos().stream()
                .map(this::paraResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable UUID id) {
        Usuario usuario = buscaUsuariosUseCase.buscarPorId(id);

        return paraResponse(usuario);
    }

    private UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}