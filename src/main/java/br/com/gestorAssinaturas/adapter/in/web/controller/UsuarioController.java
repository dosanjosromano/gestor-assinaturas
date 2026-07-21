package br.com.gestorAssinaturas.adapter.in.web.controller;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.AtualizarUsuarioRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarUsuarioRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.UsuarioResponse;
import br.com.gestorAssinaturas.application.port.in.AtualizarUsuarioCommand;
import br.com.gestorAssinaturas.application.port.in.useCase.AtualizarUsuarioUseCase;
import br.com.gestorAssinaturas.application.port.in.useCase.BuscaUsuariosUseCase;
import br.com.gestorAssinaturas.application.port.in.CriarUsuarioCommand;
import br.com.gestorAssinaturas.application.port.in.useCase.CriarUsuarioUseCase;
import br.com.gestorAssinaturas.application.port.in.useCase.ExcluirUsuarioUseCase;
import br.com.gestorAssinaturas.domain.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final CriarUsuarioUseCase criarUsuarioUseCase;
    private final BuscaUsuariosUseCase buscaUsuariosUseCase;
    private final AtualizarUsuarioUseCase atualizarUsuarioUseCase;
    private final ExcluirUsuarioUseCase excluirUsuarioUseCase;

    public UsuarioController(
            CriarUsuarioUseCase criarUsuarioUseCase,
            BuscaUsuariosUseCase buscaUsuariosUseCase,
            AtualizarUsuarioUseCase atualizarUsuarioUseCase,
            ExcluirUsuarioUseCase excluirUsuarioUseCase) {
        this.criarUsuarioUseCase = criarUsuarioUseCase;
        this.buscaUsuariosUseCase = buscaUsuariosUseCase;
        this.atualizarUsuarioUseCase = atualizarUsuarioUseCase;
        this.excluirUsuarioUseCase = excluirUsuarioUseCase;
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

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioRequest request) {
        Usuario usuario = atualizarUsuarioUseCase.atualizar(
                new AtualizarUsuarioCommand(id, request.nome(), request.email()));

        return paraResponse(usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        excluirUsuarioUseCase.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
