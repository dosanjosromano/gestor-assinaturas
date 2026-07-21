package br.com.gestorAssinaturas.adapter.in.web.controller;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarAssinaturaRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.AssinaturaResponse;
import br.com.gestorAssinaturas.application.port.in.AssinaturaAtivaResultado;
import br.com.gestorAssinaturas.application.port.in.AssinaturaResultado;
import br.com.gestorAssinaturas.application.port.in.useCase.BuscaAssinaturasUseCase;
import br.com.gestorAssinaturas.application.port.in.CriarAssinaturaCommand;
import br.com.gestorAssinaturas.application.port.in.useCase.BuscarAssinaturaAtivaUseCase;
import br.com.gestorAssinaturas.application.port.in.useCase.CancelarAssinaturaUseCase;
import br.com.gestorAssinaturas.application.port.in.useCase.CriarAssinaturaUseCase;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import br.com.gestorAssinaturas.domain.model.StatusTentativa;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/assinaturas")
public class AssinaturaController {

    private final CriarAssinaturaUseCase criarAssinaturaUseCase;
    private final BuscaAssinaturasUseCase buscaAssinaturasUseCase;
    private final BuscarAssinaturaAtivaUseCase buscarAssinaturaAtivaUseCase;
    private final CancelarAssinaturaUseCase cancelarAssinaturaUseCase;

    public AssinaturaController(
            CriarAssinaturaUseCase criarAssinaturaUseCase, BuscaAssinaturasUseCase buscaAssinaturasUseCase,
            BuscarAssinaturaAtivaUseCase buscarAssinaturaAtivaUseCase,
            CancelarAssinaturaUseCase cancelarAssinaturaUseCase) {
        this.criarAssinaturaUseCase = criarAssinaturaUseCase;
        this.buscaAssinaturasUseCase = buscaAssinaturasUseCase;
        this.buscarAssinaturaAtivaUseCase = buscarAssinaturaAtivaUseCase;
        this.cancelarAssinaturaUseCase = cancelarAssinaturaUseCase;

    }


    @Operation(summary = "Cria uma assinatura e processa a cobrança inicial")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento aprovado — assinatura ATIVA"),
            @ApiResponse(responseCode = "202",
                    description = "Pagamento ainda não confirmado (erro técnico do gateway) — assinatura "
                            + "AGUARDANDO_PAGAMENTO, tentativa fica indeterminada"),
            @ApiResponse(responseCode = "402", description = "Pagamento recusado pelo gateway"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409",
                    description = "Usuário já possui assinatura ativa/pendente, ou usuário está inativo")
    })
    @PostMapping
    public ResponseEntity<AssinaturaResponse> criar(@Valid @RequestBody CriarAssinaturaRequest request) {
        AssinaturaResultado resultado = criarAssinaturaUseCase.criar(
                new CriarAssinaturaCommand(request.usuarioId(), request.plano()));

        HttpStatus httpStatus = resultado.status() == StatusAssinatura.ATIVA
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;

        AssinaturaResponse response = new AssinaturaResponse(
                resultado.assinaturaId(),
                request.usuarioId(),
                request.plano().name(),
                statusExterno(resultado),
                resultado.dataInicio(),
                resultado.dataExpiracao());

        return ResponseEntity.status(httpStatus).body(response);
    }


    @Operation(summary = "Busca uma assinatura pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura encontrada"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    @GetMapping("/{id}")
    public AssinaturaResponse buscarPorId(@PathVariable UUID id) {
        Assinatura assinatura = buscaAssinaturasUseCase.buscarPorId(id);

        return new AssinaturaResponse(
                assinatura.getId(),
                assinatura.getUsuarioId(),
                assinatura.getPlano().name(),
                assinatura.getStatus().name(),
                assinatura.getDataInicio(),
                assinatura.getDataExpiracao());
    }


    @Operation(summary = "Busca a assinatura ativa de um usuário (cache-aside)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura ativa encontrada"),
            @ApiResponse(responseCode = "404", description = "Usuário não possui assinatura ativa")
    })
    @GetMapping("/usuario/{usuarioId}")
    public AssinaturaResponse buscarAtivaPorUsuario(@PathVariable UUID usuarioId) {
        AssinaturaAtivaResultado resultado = buscarAssinaturaAtivaUseCase.buscarAtiva(usuarioId);

        return new AssinaturaResponse(
                resultado.assinaturaId(),
                resultado.usuarioId(),
                resultado.plano().name(),
                resultado.status().name(),
                resultado.dataInicio(),
                resultado.dataExpiracao());
    }



    @Operation(summary = "Cancela uma assinatura ativa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura cancelada"),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada para o usuário informado"),
            @ApiResponse(responseCode = "409", description = "Assinatura não está ATIVA (só pode ser cancelada a partir desse status)")
    })
    @DeleteMapping("/{id}/cancelamento")
    public AssinaturaResponse cancelar(@PathVariable UUID id, @RequestHeader("X-Usuario-Id") UUID usuarioId) {
        Assinatura assinatura = cancelarAssinaturaUseCase.cancelar(id, usuarioId);

        return new AssinaturaResponse(
                assinatura.getId(),
                assinatura.getUsuarioId(),
                assinatura.getPlano().name(),
                assinatura.getStatus().name(),
                assinatura.getDataInicio(),
                assinatura.getDataExpiracao());
    }

    private String statusExterno(AssinaturaResultado resultado) {
        if (resultado.status() == StatusAssinatura.AGUARDANDO_PAGAMENTO
                && resultado.statusTentativaPagamento() == StatusTentativa.INDETERMINADA) {
            return "PROCESSANDO";
        }
        return resultado.status().name();
    }
}
