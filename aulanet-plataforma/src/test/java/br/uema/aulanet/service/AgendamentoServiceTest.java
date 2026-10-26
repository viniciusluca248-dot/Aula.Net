package br.uema.aulanet.service;

import br.uema.aulanet.dto.AgendamentoRequest;
import br.uema.aulanet.exception.AcessoNegadoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.*;
import br.uema.aulanet.repository.AgendamentoRepository;
import br.uema.aulanet.repository.AvaliacaoRepository;
import br.uema.aulanet.repository.OfertaAulaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

    @Mock AgendamentoRepository agendamentoRepository;
    @Mock OfertaAulaRepository ofertaRepository;
    @Mock AvaliacaoRepository avaliacaoRepository;
    @Mock UsuarioService usuarioService;
    @Mock FinanceiroService financeiroService;

    AgendamentoService service;
    OfertaAula oferta;
    Usuario aluno;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(HOJE.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new AgendamentoService(agendamentoRepository, ofertaRepository, avaliacaoRepository,
                usuarioService, financeiroService, clock);
        Usuario prof = new Usuario("Prof", "p@x.com", "h", null, null, Papel.PROFESSOR);
        prof.setId(1);
        aluno = new Usuario("Aluno", "a@x.com", "h", null, null, Papel.ALUNO);
        aluno.setId(2);
        oferta = new OfertaAula();
        oferta.setId(3);
        oferta.setProfessor(prof);
        oferta.setDisciplina(new Disciplina("Física"));
        oferta.setModalidade(Modalidade.PRESENCIAL);
        oferta.setValorHora(new BigDecimal("60.00"));
        oferta.setPercentualComissao(BigDecimal.TEN);
    }

    private Agendamento agendamento(int id, StatusAgendamento status, LocalTime inicio, int minutos) {
        Agendamento a = new Agendamento();
        a.setId(id);
        a.setAluno(aluno);
        a.setOferta(oferta);
        a.setDataAula(HOJE.plusDays(1));
        a.setHorarioInicio(inicio);
        a.setDuracaoMinutos(minutos);
        a.setStatus(status);
        return a;
    }

    @Test
    void solicitarCriaAgendamentoSolicitado() {
        when(ofertaRepository.findById(3)).thenReturn(Optional.of(oferta));
        when(usuarioService.obter(2)).thenReturn(aluno);
        when(agendamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.solicitar(2, new AgendamentoRequest(3, HOJE.plusDays(1), LocalTime.of(14, 0), 90));

        assertThat(dto.status()).isEqualTo(StatusAgendamento.SOLICITADO);
        assertThat(dto.valorEstimado()).isEqualByComparingTo("90.00");
    }

    @Test
    void naoPermiteAgendarNoPassado() {
        when(ofertaRepository.findById(3)).thenReturn(Optional.of(oferta));
        assertThatThrownBy(() -> service.solicitar(2, new AgendamentoRequest(3, HOJE, LocalTime.of(7, 0), 60)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void confirmarDetectaConflitoDeHorario() {
        Agendamento novo = agendamento(10, StatusAgendamento.SOLICITADO, LocalTime.of(14, 0), 60);
        Agendamento existente = agendamento(11, StatusAgendamento.CONFIRMADO, LocalTime.of(14, 30), 60);
        when(agendamentoRepository.findById(10)).thenReturn(Optional.of(novo));
        when(agendamentoRepository.findByProfessorDataStatus(1, novo.getDataAula(), StatusAgendamento.CONFIRMADO))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.confirmar(1, 10)).isInstanceOf(RegraNegocioException.class);
        assertThat(novo.getStatus()).isEqualTo(StatusAgendamento.SOLICITADO);
    }

    @Test
    void professorDeOutraAulaNaoPodeConfirmar() {
        when(agendamentoRepository.findById(10))
                .thenReturn(Optional.of(agendamento(10, StatusAgendamento.SOLICITADO, LocalTime.NOON, 60)));
        assertThatThrownBy(() -> service.confirmar(99, 10)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void recusarMudaParaCancelado() {
        Agendamento a = agendamento(10, StatusAgendamento.SOLICITADO, LocalTime.NOON, 60);
        when(agendamentoRepository.findById(10)).thenReturn(Optional.of(a));
        assertThat(service.recusar(1, 10).status()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void naoConcluiAulaQueAindaNaoComecou() {
        Agendamento futura = agendamento(10, StatusAgendamento.CONFIRMADO, LocalTime.NOON, 60);
        when(agendamentoRepository.findById(10)).thenReturn(Optional.of(futura));

        assertThatThrownBy(() -> service.concluir(1, 10)).isInstanceOf(RegraNegocioException.class);
        assertThat(futura.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verifyNoInteractions(financeiroService);
    }

    @Test
    void concluirExigeConfirmadoEGeraPagamento() {
        Agendamento solicitado = agendamento(10, StatusAgendamento.SOLICITADO, LocalTime.NOON, 60);
        solicitado.setDataAula(HOJE.minusDays(1));
        when(agendamentoRepository.findById(10)).thenReturn(Optional.of(solicitado));
        assertThatThrownBy(() -> service.concluir(1, 10)).isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(financeiroService);

        solicitado.setStatus(StatusAgendamento.CONFIRMADO);
        assertThat(service.concluir(1, 10).status()).isEqualTo(StatusAgendamento.CONCLUIDO);
        verify(financeiroService).gerarPagamento(solicitado);
    }
}
