package br.com.gestorAssinaturas.adapter.in.web.controller;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarAssinaturaRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.AssinaturaResponse;
import br.com.gestorAssinaturas.application.port.in.AssinaturaResultado;
import br.com.gestorAssinaturas.application.port.in.useCase.BuscaAssinaturasUseCase;
import br.com.gestorAssinaturas.application.port.in.CriarAssinaturaCommand;
import br.com.gestorAssinaturas.application.port.in.useCase.CriarAssinaturaUseCase;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import br.com.gestorAssinaturas.domain.model.StatusTentativa;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/assinaturas")
public class AssinaturaController {

    private final CriarAssinaturaUseCase criarAssinaturaUseCase;
    private final BuscaAssinaturasUseCase buscaAssinaturasUseCase;

    public AssinaturaController(
            CriarAssinaturaUseCase criarAssinaturaUseCase, BuscaAssinaturasUseCase buscaAssinaturasUseCase) {
        this.criarAssinaturaUseCase = criarAssinaturaUseCase;
        this.buscaAssinaturasUseCase = buscaAssinaturasUseCase;
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

    private String statusExterno(AssinaturaResultado resultado) {
        if (resultado.status() == StatusAssinatura.AGUARDANDO_PAGAMENTO
                && resultado.statusTentativaPagamento() == StatusTentativa.INDETERMINADA) {
            return "PROCESSANDO";
        }
        return resultado.status().name();
    }
}
