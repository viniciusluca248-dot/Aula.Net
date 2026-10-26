package br.uema.aulanet.repository;

import br.uema.aulanet.model.Modalidade;
import br.uema.aulanet.model.OfertaAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OfertaAulaRepository extends JpaRepository<OfertaAula, Integer> {

    @Query("""
            select o from OfertaAula o
              join fetch o.professor p
              join fetch o.disciplina d
             where (:disciplinaId is null or d.id = :disciplinaId)
               and (:modalidade is null or o.modalidade = :modalidade)
               and (:precoMin is null or o.valorHora >= :precoMin)
               and (:precoMax is null or o.valorHora <= :precoMax)
               and (:termo is null or lower(p.nome) like lower(concat('%', :termo, '%')))
             order by o.valorHora asc
            """)
    List<OfertaAula> buscar(@Param("disciplinaId") Integer disciplinaId,
                            @Param("modalidade") Modalidade modalidade,
                            @Param("precoMin") BigDecimal precoMin,
                            @Param("precoMax") BigDecimal precoMax,
                            @Param("termo") String termo);

    @Query("select o from OfertaAula o join fetch o.disciplina join fetch o.professor where o.professor.id = :professorId order by o.disciplina.nome")
    List<OfertaAula> findByProfessorId(@Param("professorId") Integer professorId);

    @Query("select o from OfertaAula o join fetch o.disciplina join fetch o.professor order by o.professor.nome")
    List<OfertaAula> findAllComDetalhes();
}
