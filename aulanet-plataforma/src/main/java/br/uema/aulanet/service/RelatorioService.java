package br.uema.aulanet.service;

import br.uema.aulanet.dto.*;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.*;
import br.uema.aulanet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Predicate;

/** Relatórios: painel financeiro do professor (RF15), histórico (RF17) e visão global do admin (RF18). */
@Service
public class RelatorioService {

    private final PagamentoRepository pagamentoRepository;
    private final RepasseRepository repasseRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParcelaRepository parcelaRepository;
    private final Clock clock;

    public RelatorioService(PagamentoRepository pagamentoRepository, RepasseRepository repasseRepository,
                            AgendamentoRepository agendamentoRepository, UsuarioRepository usuarioRepository,
                            ParcelaRepository parcelaRepository, Clock clock) {
        this.pagamentoRepository = pagamentoRepository;
        this.repasseRepository = repasseRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.parcelaRepository = parcelaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PainelFinanceiroDTO painelProfessor(Integer professorId, Integer ano, Integer mes) {
        YearMonth ref = (ano != null && mes != null) ? YearMonth.of(ano, mes) : YearMonth.now(clock);
        List<Repasse> repasses = repasseRepository.findByProfessor(professorId);

        Predicate<Repasse> doMes = r -> YearMonth.from(r.getPagamento().getAgendamento().getDataAula()).equals(ref);
        BigDecimal receitaMes = soma(repasses.stream().filter(doMes).map(Repasse::getValorBruto).toList());
        BigDecimal liquidoMes = soma(repasses.stream().filter(doMes).map(Repasse::getValorLiquido).toList());
        BigDecimal realizados = soma(repasses.stream().filter(r -> r.getStatus() == StatusRepasse.REALIZADO)
                .map(Repasse::getValorLiquido).toList());
        BigDecimal pendentes = soma(repasses.stream().filter(r -> r.getStatus() == StatusRepasse.PENDENTE)
                .map(Repasse::getValorLiquido).toList());

        List<PainelFinanceiroDTO.ReceitaMensal> ultimos = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = ref.minusMonths(i);
            ultimos.add(new PainelFinanceiroDTO.ReceitaMensal(
                    String.format("%02d/%d", ym.getMonthValue(), ym.getYear()),
                    soma(repasses.stream()
                            .filter(r -> YearMonth.from(r.getPagamento().getAgendamento().getDataAula()).equals(ym))
                            .map(Repasse::getValorBruto).toList())));
        }

        List<PainelFinanceiroDTO.ParcelaAberta> abertas = new ArrayList<>();
        for (Pagamento p : pagamentoRepository.findByProfessor(professorId)) {
            for (Parcela pa : p.getParcelas()) {
                if (pa.getStatus() != StatusParcela.PAGO) {
                    abertas.add(new PainelFinanceiroDTO.ParcelaAberta(pa.getId(),
                            p.getAgendamento().getAluno().getNome(),
                            p.getAgendamento().getOferta().getDisciplina().getNome(),
                            pa.getNumeroParcela(), p.getQtdParcelas(), pa.getValorAPagar(),
                            pa.getDataVencimento(), pa.getStatus().name()));
                }
            }
        }
        abertas.sort(Comparator.comparing(PainelFinanceiroDTO.ParcelaAberta::dataVencimento));

        return new PainelFinanceiroDTO(ref.getYear(), ref.getMonthValue(), receitaMes, liquidoMes, realizados,
                pendentes, soma(abertas.stream().map(PainelFinanceiroDTO.ParcelaAberta::valorAPagar).toList()),
                repasses.stream().filter(doMes).count(), ultimos, abertas,
                repasses.stream().map(RepasseDTO::de).toList());
    }

    @Transactional(readOnly = true)
    public HistoricoDTO historicoProfessor(Integer professorId, LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ini = inicio != null ? inicio : hoje.withDayOfMonth(1);
        LocalDate f = fim != null ? fim : hoje.withDayOfMonth(hoje.lengthOfMonth());
        if (f.isBefore(ini)) {
            throw new RegraNegocioException("A data final deve ser posterior à inicial.");
        }
        List<Agendamento> aulas = agendamentoRepository.findByProfessorPeriodo(professorId, ini, f, null);
        Map<StatusAgendamento, Long> cont = new EnumMap<>(StatusAgendamento.class);
        aulas.forEach(a -> cont.merge(a.getStatus(), 1L, Long::sum));
        List<Agendamento> concluidas = aulas.stream().filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO).toList();
        BigDecimal horas = BigDecimal.valueOf(concluidas.stream().mapToInt(Agendamento::getDuracaoMinutos).sum())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);
        BigDecimal receita = soma(concluidas.stream().map(AgendamentoDTO::valorDaAula).toList());
        return new HistoricoDTO(ini, f, aulas.size(),
                cont.getOrDefault(StatusAgendamento.CONCLUIDO, 0L),
                cont.getOrDefault(StatusAgendamento.CONFIRMADO, 0L),
                cont.getOrDefault(StatusAgendamento.SOLICITADO, 0L),
                cont.getOrDefault(StatusAgendamento.CANCELADO, 0L),
                horas, receita, aulas.stream().map(a -> AgendamentoDTO.de(a, false)).toList());
    }

    @Transactional(readOnly = true)
    public RelatorioAdminDTO relatorioAdmin() {
        Map<String, Long> porStatus = new LinkedHashMap<>();
        long total = 0;
        for (StatusAgendamento s : StatusAgendamento.values()) {
            long c = agendamentoRepository.countByStatus(s);
            porStatus.put(s.name(), c);
            total += c;
        }
        long atrasadas = parcelaRepository.countByStatus(StatusParcela.ATRASADO);
        return new RelatorioAdminDTO(usuarioRepository.count(), usuarioRepository.countByPapel(Papel.ALUNO),
                usuarioRepository.countByPapel(Papel.PROFESSOR), total, porStatus, repasseRepository.somaComissoes(),
                repasseRepository.countByStatus(StatusRepasse.PENDENTE),
                repasseRepository.somaLiquidoPorStatus(StatusRepasse.PENDENTE),
                repasseRepository.somaLiquidoPorStatus(StatusRepasse.REALIZADO), atrasadas);
    }

    private static BigDecimal soma(Collection<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_EVEN);
    }
}
