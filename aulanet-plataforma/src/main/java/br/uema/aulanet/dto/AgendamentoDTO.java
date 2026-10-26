package br.uema.aulanet.dto;

import br.uema.aulanet.model.Agendamento;
import br.uema.aulanet.model.Modalidade;
import br.uema.aulanet.model.StatusAgendamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoDTO(Integer id, Integer alunoId, String alunoNome, Integer professorId, String professorNome,
                             Integer ofertaId, String disciplina, Modalidade modalidade, LocalDate dataAula,
                             LocalTime horarioInicio, LocalTime horarioFim, Integer duracaoMinutos,
                             StatusAgendamento status, BigDecimal valorEstimado, boolean avaliado) {

    public static AgendamentoDTO de(Agendamento a, boolean avaliado) {
        var o = a.getOferta();
        return new AgendamentoDTO(a.getId(), a.getAluno().getId(), a.getAluno().getNome(),
                o.getProfessor().getId(), o.getProfessor().getNome(), o.getId(), o.getDisciplina().getNome(),
                o.getModalidade(), a.getDataAula(), a.getHorarioInicio(), a.getHorarioFim(), a.getDuracaoMinutos(),
                a.getStatus(), valorDaAula(a), avaliado);
    }

    /** valor/hora × duração em horas. */
    public static BigDecimal valorDaAula(Agendamento a) {
        return a.getOferta().getValorHora()
                .multiply(BigDecimal.valueOf(a.getDuracaoMinutos()))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);
    }
}
