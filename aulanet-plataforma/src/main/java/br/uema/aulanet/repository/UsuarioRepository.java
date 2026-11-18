package br.uema.aulanet.repository;

import br.uema.aulanet.model.Papel;
import br.uema.aulanet.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    List<Usuario> findByPapelOrderByNomeAsc(Papel papel);

    long countByPapel(Papel papel);
}
