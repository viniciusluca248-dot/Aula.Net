package br.uema.aulanet.repository;

import br.uema.aulanet.model.Repasse;
import br.uema.aulanet.model.StatusRepasse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RepasseRepository extends JpaRepository<Repasse, Integer> {

    String FETCH = "select r from Repasse r join fetch r.professor join fetch r.pagamento p join fetch p.agendamento a join fetch a.aluno join fetch a.oferta o join fetch o.disciplina ";

    @Query(FETCH + "where r.professor.id = :professorId order by r.id desc")
    List<Repasse> findByProfessor(@Param("professorId") Integer professorId);

    @Query(FETCH + "order by r.id desc")
    List<Repasse> findAllDetalhado();

    Optional<Repasse> findByPagamentoId(Integer pagamentoId);

    @Query("select coalesce(sum(r.valorComissao), 0) from Repasse r")
    BigDecimal somaComissoes();

    @Query("select coalesce(sum(r.valorLiquido), 0) from Repasse r where r.status = :status")
    BigDecimal somaLiquidoPorStatus(@Param("status") StatusRepasse status);

    long countByStatus(StatusRepasse status);
}
