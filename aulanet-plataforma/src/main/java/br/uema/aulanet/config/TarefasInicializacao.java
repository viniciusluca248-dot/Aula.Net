package br.uema.aulanet.config;

import br.uema.aulanet.service.FinanceiroService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Garante que multas de parcelas vencidas estejam atualizadas logo após a aplicação subir. */
@Component
public class TarefasInicializacao {

    private final FinanceiroService financeiroService;

    public TarefasInicializacao(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        financeiroService.processarAtrasos();
    }
}
