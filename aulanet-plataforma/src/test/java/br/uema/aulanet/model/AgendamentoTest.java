package br.uema.aulanet.model;

import br.uema.aulanet.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Critério de aceitação (a): só a sequência SOLICITADO → CONFIRMADO → CONCLUIDO é aceita. */
class AgendamentoTest {

    private Agendamento com(StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setStatus(status);
        return a;
    }

    @Test
    void sequenciaValidaAteConcluido() {
        Agendamento a = com(StatusAgendamento.SOLICITADO);
        a.confirmar();
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        a.concluir();
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }

    @Test
    void naoConcluiSemConfirmar() {
        assertThatThrownBy(() -> com(StatusAgendamento.SOLICITADO).concluir()).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoConfirmaNemRecusaDuasVezes() {
        assertThatThrownBy(() -> com(StatusAgendamento.CONFIRMADO).confirmar()).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> com(StatusAgendamento.CANCELADO).recusar()).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void aulaConcluidaOuCanceladaNaoMudaMais() {
        for (StatusAgendamento final_ : new StatusAgendamento[]{StatusAgendamento.CONCLUIDO, StatusAgendamento.CANCELADO}) {
            assertThatThrownBy(() -> com(final_).confirmar()).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> com(final_).concluir()).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> com(final_).cancelar()).isInstanceOf(RegraNegocioException.class);
        }
    }
}
