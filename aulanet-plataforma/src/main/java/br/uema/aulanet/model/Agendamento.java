package br.uema.aulanet.model;

import br.uema.aulanet.exception.RegraNegocioException;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Fluxo de três etapas: SOLICITADO → CONFIRMADO → CONCLUIDO (ou CANCELADO). */
@Entity
@Table(name = "agendamento", indexes = {
        @Index(name = "idx_agendamento_data", columnList = "data_aula"),
        @Index(name = "idx_agendamento_status", columnList = "status")
})
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agendamento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aluno", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta", nullable = false)
    private OfertaAula oferta;

    @Column(name = "data_aula", nullable = false)
    private LocalDate dataAula;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusAgendamento status = StatusAgendamento.SOLICITADO;

    // As transições de status ficam na própria entidade, para que nenhuma camada consiga contorná-las.

    public void confirmar() {
        exigirStatus(StatusAgendamento.SOLICITADO);
        this.status = StatusAgendamento.CONFIRMADO;
    }

    public void recusar() {
        exigirStatus(StatusAgendamento.SOLICITADO);
        this.status = StatusAgendamento.CANCELADO;
    }

    public void concluir() {
        exigirStatus(StatusAgendamento.CONFIRMADO);
        this.status = StatusAgendamento.CONCLUIDO;
    }

    /** Cancelamento pelo aluno: permitido enquanto a aula está solicitada ou confirmada. */
    public void cancelar() {
        if (status != StatusAgendamento.SOLICITADO && status != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Somente aulas solicitadas ou confirmadas podem ser canceladas.");
        }
        this.status = StatusAgendamento.CANCELADO;
    }

    private void exigirStatus(StatusAgendamento esperado) {
        if (this.status != esperado) {
            throw new RegraNegocioException(
                    "Operação inválida: agendamento está " + status + ", esperado " + esperado + ".");
        }
    }

    /** Duas aulas no mesmo dia se sobrepõem quando uma começa antes de a outra terminar. */
    public boolean sobrepoe(Agendamento outro) {
        return !this.equals(outro)
                && (this.id == null || !this.id.equals(outro.getId()))
                && this.dataAula.equals(outro.getDataAula())
                && this.horarioInicio.isBefore(outro.getHorarioFim())
                && outro.getHorarioInicio().isBefore(this.getHorarioFim());
    }

    public LocalTime getHorarioFim() {
        return horarioInicio.plusMinutes(duracaoMinutos);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Usuario getAluno() { return aluno; }
    public void setAluno(Usuario aluno) { this.aluno = aluno; }
    public OfertaAula getOferta() { return oferta; }
    public void setOferta(OfertaAula oferta) { this.oferta = oferta; }
    public LocalDate getDataAula() { return dataAula; }
    public void setDataAula(LocalDate dataAula) { this.dataAula = dataAula; }
    public LocalTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalTime horarioInicio) { this.horarioInicio = horarioInicio; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
}
