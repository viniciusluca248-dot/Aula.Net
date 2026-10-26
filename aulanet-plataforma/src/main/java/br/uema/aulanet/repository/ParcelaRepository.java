package br.uema.aulanet.repository;

import br.uema.aulanet.model.Parcela;
import br.uema.aulanet.model.StatusParcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ParcelaRepository extends JpaRepository<Parcela, Integer> {

    /** Parcelas não pagas cujo vencimento já passou (usada pela tarefa agendada de multa). */
    @Query("select p from Parcela p where p.status in :status and p.dataVencimento < :hoje")
    List<Parcela> findVencidas(@Param("status") Collection<StatusParcela> status, @Param("hoje") LocalDate hoje);

    long countByStatus(StatusParcela status);
}
