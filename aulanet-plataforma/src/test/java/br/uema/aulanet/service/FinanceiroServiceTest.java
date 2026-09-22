package br.uema.aulanet.service;

import br.uema.aulanet.config.AulanetProperties;
import br.uema.aulanet.exception.AcessoNegadoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.*;
import br.uema.aulanet.repository.PagamentoRepository;
import br.uema.aulanet.repository.ParcelaRepository;
import br.uema.aulanet.repository.RepasseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceiroServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

    @Mock PagamentoRepository pagamentoRepository;
    @Mock ParcelaRepository parcelaRepository;
    @Mock RepasseRepository repasseRepository;

    FinanceiroService service;

    @BeforeEach
    void setUp() {
        var calc = new CalculadoraFinanceira(new AulanetProperties(new AulanetProperties.Jwt("x", 8),
                new AulanetProperties.Financeiro(new BigDecimal("0.0199"), 12, new BigDecimal("0.02"),
                        new BigDecimal("0.00033"), new BigDecimal("10"), 7)));
        Clock clock = Clock.fixed(HOJE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new FinanceiroService(pagamentoRepository, parcelaRepository, repasseRepository, calc, clock);
    }

    private Agendamento aula(int minutos, String valorHora, String comissao) {
        Usuario prof = new Usuario("Prof", "p@x.com", "h", null, null, Papel.PROFESSOR);
        prof.setId(1);
        Usuario aluno = new Usuario("Aluno", "a@x.com", "h", null, null, Papel.ALUNO);
        aluno.setId(2);
        OfertaAula o = new OfertaAula();
        o.setProfessor(prof);
        o.setDisciplina(new Disciplina("Matemática"));
        o.setModalidade(Modalidade.ONLINE);
        o.setValorHora(new BigDecimal(valorHora));
        o.setPercentualComissao(new BigDecimal(comissao));
        Agendamento a = new Agendamento();
        a.setId(10);
        a.setAluno(aluno);
        a.setOferta(o);
        a.setDataAula(HOJE);
        a.setHorarioInicio(LocalTime.of(10, 0));
        a.setDuracaoMinutos(minutos);
        a.setStatus(StatusAgendamento.CONCLUIDO);
        return a;
    }

    @Test
    void gerarPagamentoCriaParcelaUnicaERepassePendente() {
        Pagamento p = service.gerarPagamento(aula(90, "80.00", "10.00"));

        assertThat(p.getValorTotal()).isEqualByComparingTo("120.00");
        assertThat(p.getTipo()).isEqualTo(TipoPagamento.A_VISTA);
        assertThat(p.getParcelas()).singleElement().satisfies(x -> {
            assertThat(x.getValor()).isEqualByComparingTo("120.00");
            assertThat(x.getDataVencimento()).isEqualTo(HOJE.plusDays(7));
        });
        ArgumentCaptor<Repasse> cap = ArgumentCaptor.forClass(Repasse.class);
        verify(repasseRepository).save(cap.capture());
        assertThat(cap.getValue().getValorComissao()).isEqualByComparingTo("12.00");
        assertThat(cap.getValue().getValorLiquido()).isEqualByComparingTo("108.00");
        assertThat(cap.getValue().getStatus()).isEqualTo(StatusRepasse.PENDENTE);
    }

    @Test
    void naoGeraPagamentoDuplicado() {
        when(pagamentoRepository.existsByAgendamentoId(anyInt())).thenReturn(true);
        assertThatThrownBy(() -> service.gerarPagamento(aula(60, "50", "10"))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void parcelarGeraVencimentosMensais() {
        Pagamento p = service.gerarPagamento(aula(180, "100.00", "10"));
        p.setId(5);
        when(pagamentoRepository.findDetalhado(5)).thenReturn(Optional.of(p));

        var dto = service.parcelar(2, 5, 3);

        assertThat(dto.tipo()).isEqualTo(TipoPagamento.PARCELADO);
        assertThat(dto.parcelas()).hasSize(3);
        assertThat(dto.parcelas().get(1).dataVencimento()).isEqualTo(HOJE.plusDays(7).plusMonths(1));
        assertThat(dto.parcelas().get(0).valor()).isEqualByComparingTo("104.01");
    }

    @Test
    void outroAlunoNaoPodeParcelar() {
        Pagamento p = service.gerarPagamento(aula(60, "100.00", "10"));
        when(pagamentoRepository.findDetalhado(5)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.parcelar(99, 5, 2)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void pagarUltimaParcelaRealizaRepasse() {
        Pagamento p = service.gerarPagamento(aula(60, "100.00", "10"));
        p.setId(5);
        Parcela parcela = p.getParcelas().get(0);
        parcela.setId(7);
        Repasse r = new Repasse();
        when(parcelaRepository.findById(7)).thenReturn(Optional.of(parcela));
        when(pagamentoRepository.findDetalhado(5)).thenReturn(Optional.of(p));
        when(repasseRepository.findByPagamentoId(5)).thenReturn(Optional.of(r));

        var dto = service.pagarParcela(2, 7);

        assertThat(dto.quitado()).isTrue();
        assertThat(r.getStatus()).isEqualTo(StatusRepasse.REALIZADO);
    }

    @Test
    void tarefaDiariaMarcaAtrasoECalculaMulta() {
        Parcela vencida = new Parcela();
        vencida.setValor(new BigDecimal("100.00"));
        vencida.setDataVencimento(HOJE.minusDays(10));
        vencida.setStatus(StatusParcela.PENDENTE);
        when(parcelaRepository.findVencidas(any(), any())).thenReturn(List.of(vencida));

        int n = service.processarAtrasos();

        assertThat(n).isEqualTo(1);
        assertThat(vencida.getStatus()).isEqualTo(StatusParcela.ATRASADO);
        assertThat(vencida.getValorMulta()).isEqualByComparingTo("2.33");
        assertThat(vencida.getValorAPagar()).isEqualByComparingTo("102.33");
    }
}
