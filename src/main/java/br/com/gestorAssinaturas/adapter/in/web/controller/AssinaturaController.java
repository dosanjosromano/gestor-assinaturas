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
