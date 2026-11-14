package br.uema.aulanet.repository;

import br.uema.aulanet.model.Parcela;
import br.uema.aulanet.model.StatusParcela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ParcelaRepository extends JpaRepository<Parcela, Integer> {

    /** Parcelas não pagas cujo vencimento já passou (usada pela rotina diária de encargos). */
    List<Parcela> findByStatusInAndDataVencimentoBefore(Collection<StatusParcela> status, LocalDate hoje);

    long countByStatus(StatusParcela status);
}
