package br.uema.aulanet.repository;

import br.uema.aulanet.model.Agendamento;
import br.uema.aulanet.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Integer> {

    String FETCH = "select a from Agendamento a join fetch a.aluno join fetch a.oferta o join fetch o.professor join fetch o.disciplina ";

    @Query(FETCH + "where a.aluno.id = :alunoId order by a.dataAula desc, a.horarioInicio desc")
    List<Agendamento> findByAluno(@Param("alunoId") Integer alunoId);

    @Query(FETCH + "where o.professor.id = :professorId order by a.dataAula desc, a.horarioInicio desc")
    List<Agendamento> findByProfessor(@Param("professorId") Integer professorId);

    @Query(FETCH + """
            where o.professor.id = :professorId
              and a.dataAula between :inicio and :fim
              and (:status is null or a.status = :status)
            order by a.dataAula asc, a.horarioInicio asc
            """)
    List<Agendamento> findByProfessorPeriodo(@Param("professorId") Integer professorId,
                                             @Param("inicio") LocalDate inicio,
                                             @Param("fim") LocalDate fim,
                                             @Param("status") StatusAgendamento status);

    @Query(FETCH + "where o.professor.id = :professorId and a.dataAula = :data and a.status = :status")
    List<Agendamento> findByProfessorDataStatus(@Param("professorId") Integer professorId,
                                                @Param("data") LocalDate data,
                                                @Param("status") StatusAgendamento status);

    long countByStatus(StatusAgendamento status);
}
