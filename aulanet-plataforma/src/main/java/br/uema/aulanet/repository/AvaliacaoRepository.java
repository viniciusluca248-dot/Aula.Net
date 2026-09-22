package br.uema.aulanet.repository;

import br.uema.aulanet.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Integer> {

    boolean existsByAgendamentoId(Integer agendamentoId);

    @Query("select a from Avaliacao a join fetch a.aluno where a.professor.id = :professorId order by a.id desc")
    List<Avaliacao> findByProfessor(@Param("professorId") Integer professorId);

    @Query("select avg(a.nota) from Avaliacao a where a.professor.id = :professorId")
    Double mediaDoProfessor(@Param("professorId") Integer professorId);

    long countByProfessorId(Integer professorId);
}
