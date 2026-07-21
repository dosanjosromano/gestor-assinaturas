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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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


    @Operation(summary = "Cadastra um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado (ativo ou inativo)")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        Usuario usuario = criarUsuarioUseCase.criar(new CriarUsuarioCommand(request.nome(), request.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(usuario));
    }


    @Operation(summary = "Lista todos os usuários ativos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários ativos (usuários inativos não aparecem)")
    })
    @GetMapping
    public List<UsuarioResponse> listarTodos() {
        return buscaUsuariosUseCase.listarTodos().stream()
                .map(this::paraResponse)
                .toList();
    }


    @Operation(summary = "Busca um usuário pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404",
                    description = "Usuário não encontrado — inclui usuário inativo (mesmo tratamento, para não "
                            + "revelar que o id já existiu)")
    })
    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable UUID id) {
        Usuario usuario = buscaUsuariosUseCase.buscarPorId(id);

        return paraResponse(usuario);
    }


    @Operation(summary = "Atualiza nome e/ou e-mail de um usuário (campos opcionais)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado (inclui usuário inativo)"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado por outro usuário")
    })
    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioRequest request) {
        Usuario usuario = atualizarUsuarioUseCase.atualizar(
                new AtualizarUsuarioCommand(id, request.nome(), request.email()));

        return paraResponse(usuario);
    }


    @Operation(summary = "Exclui (soft delete) um usuário — idempotente")
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "Usuário excluído — inclui chamar novamente em um usuário já inativo (idempotente, não é erro)"),
            @ApiResponse(responseCode = "404", description = "Nenhum usuário com esse id jamais existiu")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        excluirUsuarioUseCase.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
