package br.uema.aulanet.controller;

import br.uema.aulanet.dto.*;
import br.uema.aulanet.security.UsuarioLogado;
import br.uema.aulanet.service.AvaliacaoService;
import br.uema.aulanet.service.FinanceiroService;
import br.uema.aulanet.service.RelatorioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PagamentoController {

    private final FinanceiroService financeiroService;
    private final RelatorioService relatorioService;
    private final AvaliacaoService avaliacaoService;

    public PagamentoController(FinanceiroService financeiroService, RelatorioService relatorioService,
                               AvaliacaoService avaliacaoService) {
        this.financeiroService = financeiroService;
        this.relatorioService = relatorioService;
        this.avaliacaoService = avaliacaoService;
    }

    /** Simulação usada pelo seletor de parcelas da tela de pagamento. */
    @GetMapping("/pagamentos/simulacao")
    public SimulacaoDTO simular(@RequestParam BigDecimal valor, @RequestParam(defaultValue = "1") int parcelas) {
        return financeiroService.simular(valor, parcelas);
    }

    @GetMapping("/pagamentos/meus")
    @PreAuthorize("hasRole('ALUNO')")
    public List<PagamentoDTO> meus(@AuthenticationPrincipal UsuarioLogado u) {
        return financeiroService.pagamentosDoAluno(u.getId());
    }

    @GetMapping("/pagamentos/{id}")
    @PreAuthorize("hasRole('ALUNO')")
    public PagamentoDTO detalhe(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        return financeiroService.pagamentoDoAluno(u.getId(), id);
    }

    @PostMapping("/pagamentos/{id}/parcelamento")
    @PreAuthorize("hasRole('ALUNO')")
    public PagamentoDTO parcelar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id,
                                 @Valid @RequestBody ParcelamentoRequest req) {
        return financeiroService.parcelar(u.getId(), id, req.qtdParcelas());
    }

    @PostMapping("/parcelas/{id}/pagar")
    @PreAuthorize("hasRole('ALUNO')")
    public PagamentoDTO pagarParcela(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        return financeiroService.pagarParcela(u.getId(), id);
    }

    @PostMapping("/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALUNO')")
    public AvaliacaoDTO avaliar(@AuthenticationPrincipal UsuarioLogado u, @Valid @RequestBody AvaliacaoRequest req) {
        return avaliacaoService.avaliar(u.getId(), req);
    }

    /** RF15: painel financeiro do professor. */
    @GetMapping("/financeiro/painel")
    @PreAuthorize("hasRole('PROFESSOR')")
    public PainelFinanceiroDTO painel(@AuthenticationPrincipal UsuarioLogado u,
                                      @RequestParam(required = false) Integer ano,
                                      @RequestParam(required = false) Integer mes) {
        return relatorioService.painelProfessor(u.getId(), ano, mes);
    }
}
