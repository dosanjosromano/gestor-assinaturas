package br.com.gestorAssinaturas.adapter.in.scheduler;

import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.RenovacaoEventPublisherPort;
import br.com.gestorAssinaturas.application.port.out.RenovacaoSolicitadaEvent;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile("worker")
public class RenovacaoScheduler {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final RenovacaoEventPublisherPort renovacaoEventPublisherPort;

    public RenovacaoScheduler(
            AssinaturaRepositoryPort assinaturaRepositoryPort,
            RenovacaoEventPublisherPort renovacaoEventPublisherPort) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
        this.renovacaoEventPublisherPort = renovacaoEventPublisherPort;
    }

    @Scheduled(cron = "${app.scheduler.renovacao-cron}")
    public void executar() {
        LocalDate hoje = LocalDate.now();
        List<Assinatura> elegiveis = assinaturaRepositoryPort.buscarElegiveisParaRenovacao(hoje);
        log.info("[inicia] busca de assinaturas elegíveis para renovação - data={} total={}", hoje, elegiveis.size());

        elegiveis.forEach(assinatura ->
                renovacaoEventPublisherPort.publicar(new RenovacaoSolicitadaEvent(assinatura.getId())));

        log.info("[finaliza] eventos de renovação publicados - total={}", elegiveis.size());
    }
}

