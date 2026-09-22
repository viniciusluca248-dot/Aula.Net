package br.uema.aulanet.model;

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
