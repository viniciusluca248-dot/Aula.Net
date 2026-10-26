package br.uema.aulanet.model;

import jakarta.persistence.*;

@Entity
@Table(name = "avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avaliacao")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false, unique = true)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aluno", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_professor", nullable = false)
    private Usuario professor;

    @Column(nullable = false)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Agendamento getAgendamento() { return agendamento; }
    public void setAgendamento(Agendamento agendamento) { this.agendamento = agendamento; }
    public Usuario getAluno() { return aluno; }
    public void setAluno(Usuario aluno) { this.aluno = aluno; }
    public Usuario getProfessor() { return professor; }
    public void setProfessor(Usuario professor) { this.professor = professor; }
    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
