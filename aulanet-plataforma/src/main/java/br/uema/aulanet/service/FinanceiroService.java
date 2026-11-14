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
import java.math.RoundingMode;
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
     * RF09 / RF12 / RF14: chamado na mesma transação da conclusão da aula. Gera o pagamento à vista
     * (1 parcela) e o repasse PENDENTE do professor. O aluno pode depois optar por parcelar.
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
        gerarRepasse(p);
        return p;
    }

    /**
     * RF14: repasse = valor bruto da aula − comissão da oferta. A base é o valor da aula, sem os juros do
     * parcelamento nem os encargos de atraso, que remuneram o financiamento e não o serviço.
     */
    private Repasse gerarRepasse(Pagamento pagamento) {
        OfertaAula oferta = pagamento.getAgendamento().getOferta();
        BigDecimal bruto = pagamento.getValorTotal();
        BigDecimal comissao = bruto.multiply(oferta.getPercentualComissao())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Repasse repasse = new Repasse();
        repasse.setPagamento(pagamento);
        repasse.setProfessor(oferta.getProfessor());
        repasse.setValorBruto(bruto);
        repasse.setValorComissao(comissao);
        repasse.setValorLiquido(bruto.subtract(comissao));
        repasse.setStatus(StatusRepasse.PENDENTE);
        return repasseRepository.save(repasse);
    }

    public SimulacaoDTO simular(BigDecimal valor, int qtdParcelas) {
        return calculadora.simular(valor, qtdParcelas);
    }

    /** RF12: aluno escolhe à vista (1) ou parcelado (2..N) enquanto nenhuma parcela foi paga nem está em atraso. */
    @Transactional
    public PagamentoDTO parcelar(Integer alunoId, Integer pagamentoId, int qtdParcelas) {
        Pagamento p = obterDoAluno(alunoId, pagamentoId);
        if (!podeParcelar(p)) {
            throw new RegraNegocioException("Não é possível alterar o parcelamento após pagar uma parcela ou com parcela em atraso.");
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
        // só a prestação e o vencimento são gravados; juros e amortização são recalculáveis pela Tabela Price
        for (LinhaAmortizacao l : sim.parcelas()) {
            Parcela parcela = new Parcela();
            parcela.setPagamento(p);
            parcela.setNumeroParcela(l.numero());
            parcela.setValor(l.valorParcela());
            parcela.setValorJuros(BigDecimal.ZERO.setScale(2));
            parcela.setValorMulta(BigDecimal.ZERO.setScale(2));
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
        aplicarEncargos(parcela, LocalDate.now(clock));
        parcela.setStatus(StatusParcela.PAGO);
        // com o pagamento quitado, o repasse fica liberado para o administrador registrar a transferência
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

    /** RF13: rotina diária que aplica multa e juros de mora às parcelas vencidas. */
    @Scheduled(cron = "${aulanet.financeiro.cron-atrasos:0 0 1 * * *}")
    @Transactional
    public int processarAtrasos() {
        LocalDate hoje = LocalDate.now(clock);
        List<Parcela> vencidas = parcelaRepository.findByStatusInAndDataVencimentoBefore(
                List.of(StatusParcela.PENDENTE, StatusParcela.ATRASADO), hoje);
        vencidas.forEach(p -> aplicarEncargos(p, hoje));
        if (!vencidas.isEmpty()) {
            log.info("Processamento de atrasos: {} parcela(s) atualizada(s).", vencidas.size());
        }
        return vencidas.size();
    }

    /**
     * A multa (valor_multa) é aplicada uma única vez, na passagem de PENDENTE para ATRASADO; os juros de mora
     * (valor_juros) são recalculados a partir do total de dias de atraso, o que torna a rotina idempotente.
     */
    void aplicarEncargos(Parcela parcela, LocalDate hoje) {
        long diasAtraso = ChronoUnit.DAYS.between(parcela.getDataVencimento(), hoje);
        if (diasAtraso <= 0 || parcela.getStatus() == StatusParcela.PAGO) {
            return;
        }
        if (parcela.getStatus() == StatusParcela.PENDENTE) {
            parcela.setValorMulta(calculadora.calcularMulta(parcela.getValor()));
            parcela.setStatus(StatusParcela.ATRASADO);
        }
        parcela.setValorJuros(calculadora.calcularJurosMora(parcela.getValor(), diasAtraso));
    }

    @Transactional
    public RepasseDTO realizarRepasse(Integer repasseId) {
        Repasse r = repasseRepository.findById(repasseId)
                .orElseThrow(() -> new NaoEncontradoException("Repasse não encontrado."));
        if (r.getStatus() == StatusRepasse.REALIZADO) {
            throw new RegraNegocioException("Este repasse já foi realizado.");
        }
        // o professor só recebe depois que o aluno quita todas as parcelas
        if (!r.getPagamento().isQuitado()) {
            throw new RegraNegocioException("O repasse só pode ser realizado depois que o aluno quitar todas as parcelas.");
        }
        r.setStatus(StatusRepasse.REALIZADO);
        return RepasseDTO.de(r);
    }

    @Transactional(readOnly = true)
    public List<RepasseDTO> todosRepasses() {
        return repasseRepository.findAllDetalhado().stream().map(RepasseDTO::de).toList();
    }

    private boolean podeParcelar(Pagamento p) {
        // parcela em atraso também bloqueia: refazer o parcelamento apagaria a multa e adiaria o vencimento
        return p.getParcelas().stream().allMatch(x -> x.getStatus() == StatusParcela.PENDENTE);
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
