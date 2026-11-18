package br.uema.aulanet.repository;

import br.uema.aulanet.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Integer> {

    List<Disciplina> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);
}
