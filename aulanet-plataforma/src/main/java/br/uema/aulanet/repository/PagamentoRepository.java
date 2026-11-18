package br.uema.aulanet.repository;

import br.uema.aulanet.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Integer> {

    String FETCH = "select distinct p from Pagamento p join fetch p.agendamento a join fetch a.aluno join fetch a.oferta o join fetch o.professor join fetch o.disciplina left join fetch p.parcelas ";

    @Query(FETCH + "where a.aluno.id = :alunoId order by p.id desc")
    List<Pagamento> findByAluno(@Param("alunoId") Integer alunoId);

    @Query(FETCH + "where o.professor.id = :professorId order by p.id desc")
    List<Pagamento> findByProfessor(@Param("professorId") Integer professorId);

    @Query(FETCH + "where p.id = :id")
    Optional<Pagamento> findDetalhado(@Param("id") Integer id);

    boolean existsByAgendamentoId(Integer agendamentoId);
}
