package br.uema.aulanet.service;

import br.uema.aulanet.dto.PagamentoDTO;
import br.uema.aulanet.dto.RepasseDTO;
import br.uema.aulanet.dto.SimulacaoDTO;
import br.uema.aulanet.dto.AgendamentoDTO;
import br.uema.aulanet.exception.AcessoNegadoException;
import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.*;
import br.uema.aulanet.repository.PagamentoRepository;
import br.uema.aulanet.repository.ParcelaRepository;
import br.uema.aulanet.repository.RepasseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Módulo financeiro: RF09, RF11, RF12, RF13 e RF14. */
@Service
public class FinanceiroService {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroService.class);

    private final PagamentoRepository pagamentoRepository;
    private final ParcelaRepository parcelaRepository;
    private final RepasseRepository repasseRepository;
    private final CalculadoraFinanceira calculadora;
    private final Clock clock;

    public FinanceiroService(PagamentoRepository pagamentoRepository, ParcelaRepository parcelaRepository,
                             RepasseRepository repasseRepository, CalculadoraFinanceira calculadora, Clock clock) {
        this.pagamentoRepository = pagamentoRepository;
        this.parcelaRepository = parcelaRepository;
        this.repasseRepository = repasseRepository;
        this.calculadora = calculadora;
        this.clock = clock;
    }

    /**
     * RF09 / RF11 / RF14: chamado quando a aula é concluída. Gera o pagamento à vista (1 parcela)
     * e o repasse PENDENTE do professor. O aluno pode depois optar por parcelar.
     */
    @Transactional
    public Pagamento gerarPagamento(Agendamento agendamento) {
        if (pagamentoRepository.existsByAgendamentoId(agendamento.getId())) {
            throw new RegraNegocioException("Esta aula já possui pagamento gerado.");
        }
        Pagamento p = new Pagamento();
        p.setAgendamento(agendamento);
        p.setValorTotal(AgendamentoDTO.valorDaAula(agendamento));
        aplicarParcelamento(p, 1);
        pagamentoRepository.save(p);

        BigDecimal[] repasse = calculadora.calcularRepasse(p.getValorTotal(),
                agendamento.getOferta().getPercentualComissao());
        Repasse r = new Repasse();
        r.setPagamento(p);
        r.setProfessor(agendamento.getOferta().getProfessor());
        r.setValorBruto(p.getValorTotal());
        r.setValorComissao(repasse[0]);
        r.setValorLiquido(repasse[1]);
        repasseRepository.save(r);
        return p;
    }

    public SimulacaoDTO simular(java.math.BigDecimal valor, int qtdParcelas) {
        return calculadora.simular(valor, qtdParcelas);
    }

    /** RF12: aluno escolhe à vista (1) ou parcelado (2..N) enquanto nenhuma parcela foi paga. */
    @Transactional
    public PagamentoDTO parcelar(Integer alunoId, Integer pagamentoId, int qtdParcelas) {
        Pagamento p = obterDoAluno(alunoId, pagamentoId);
        if (!podeParcelar(p)) {
            throw new RegraNegocioException("Não é possível alterar o parcelamento após pagar uma parcela.");
        }
        aplicarParcelamento(p, qtdParcelas);
        pagamentoRepository.flush(); // atribui os ids das novas parcelas antes de devolvê-las
        return PagamentoDTO.de(p, true);
    }

    private void aplicarParcelamento(Pagamento p, int qtdParcelas) {
        SimulacaoDTO sim = calculadora.simular(p.getValorTotal(), qtdParcelas);
        p.getParcelas().clear();
        if (p.getId() != null) {
            pagamentoRepository.flush(); // remove as parcelas antigas antes de inserir as novas
        }
        p.setTipo(qtdParcelas == 1 ? TipoPagamento.A_VISTA : TipoPagamento.PARCELADO);
        p.setQtdParcelas(qtdParcelas);
        LocalDate primeiro = LocalDate.now(clock).plusDays(calculadora.diasPrimeiroVencimento());
        for (SimulacaoDTO.Linha l : sim.parcelas()) {
            Parcela parcela = new Parcela();
            parcela.setPagamento(p);
            parcela.setNumeroParcela(l.numero());
            parcela.setValor(l.valor());
            parcela.setValorJuros(l.juros());
            parcela.setValorMulta(java.math.BigDecimal.ZERO.setScale(2));
            parcela.setDataVencimento(primeiro.plusMonths(l.numero() - 1L));
            parcela.setStatus(StatusParcela.PENDENTE);
            p.getParcelas().add(parcela);
        }
    }

    /** Registro manual do pagamento de uma parcela (sem gateway real, conforme escopo). */
    @Transactional
    public PagamentoDTO pagarParcela(Integer alunoId, Integer parcelaId) {
        Parcela parcela = parcelaRepository.findById(parcelaId)
                .orElseThrow(() -> new NaoEncontradoException("Parcela não encontrada."));
        Pagamento p = obterDoAluno(alunoId, parcela.getPagamento().getId());
        if (parcela.getStatus() == StatusParcela.PAGO) {
            throw new RegraNegocioException("Parcela já está paga.");
        }
        // encargos atualizados até hoje antes de quitar
        atualizarEncargos(parcela, LocalDate.now(clock));
        parcela.setStatus(StatusParcela.PAGO);
        if (p.isQuitado()) {
            // repasse automático: com o pagamento quitado, o valor líquido é liberado ao professor
            repasseRepository.findByPagamentoId(p.getId()).ifPresent(r -> r.setStatus(StatusRepasse.REALIZADO));
        }
        return PagamentoDTO.de(p, false);
    }

    @Transactional(readOnly = true)
    public List<PagamentoDTO> pagamentosDoAluno(Integer alunoId) {
        return pagamentoRepository.findByAluno(alunoId).stream()
                .map(p -> PagamentoDTO.de(p, podeParcelar(p))).toList();
    }

    @Transactional(readOnly = true)
    public PagamentoDTO pagamentoDoAluno(Integer alunoId, Integer pagamentoId) {
        Pagamento p = obterDoAluno(alunoId, pagamentoId);
        return PagamentoDTO.de(p, podeParcelar(p));
    }

    /** RF13: tarefa diária que marca parcelas vencidas como ATRASADO e recalcula multa + mora. */
    @Scheduled(cron = "${aulanet.financeiro.cron-atrasos:0 5 0 * * *}")
    @Transactional
    public int processarAtrasos() {
        LocalDate hoje = LocalDate.now(clock);
        List<Parcela> vencidas = parcelaRepository.findVencidas(
                List.of(StatusParcela.PENDENTE, StatusParcela.ATRASADO), hoje);
        vencidas.forEach(p -> atualizarEncargos(p, hoje));
        if (!vencidas.isEmpty()) {
            log.info("Processamento de atrasos: {} parcela(s) atualizada(s).", vencidas.size());
        }
        return vencidas.size();
    }

    void atualizarEncargos(Parcela parcela, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(parcela.getDataVencimento(), hoje);
        if (dias > 0 && parcela.getStatus() != StatusParcela.PAGO) {
            parcela.setStatus(StatusParcela.ATRASADO);
            parcela.setValorMulta(calculadora.calcularEncargosAtraso(parcela.getValor(), dias));
        }
    }

    @Transactional
    public RepasseDTO realizarRepasse(Integer repasseId) {
        Repasse r = repasseRepository.findById(repasseId)
                .orElseThrow(() -> new NaoEncontradoException("Repasse não encontrado."));
        r.setStatus(StatusRepasse.REALIZADO);
        return RepasseDTO.de(r);
    }

    @Transactional(readOnly = true)
    public List<RepasseDTO> todosRepasses() {
        return repasseRepository.findAllDetalhado().stream().map(RepasseDTO::de).toList();
    }

    private boolean podeParcelar(Pagamento p) {
        return p.getParcelas().stream().noneMatch(x -> x.getStatus() == StatusParcela.PAGO);
    }

    private Pagamento obterDoAluno(Integer alunoId, Integer pagamentoId) {
        Pagamento p = pagamentoRepository.findDetalhado(pagamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Pagamento não encontrado."));
        if (!p.getAgendamento().getAluno().getId().equals(alunoId)) {
            throw new AcessoNegadoException("o pagamento pertence a outro aluno.");
        }
        return p;
    }
}
