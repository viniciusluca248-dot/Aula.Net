package br.uema.aulanet.dto;

import br.uema.aulanet.model.Papel;
import br.uema.aulanet.model.Usuario;

public record UsuarioDTO(Integer id, String nome, String email, String cpf, String telefone, Papel papel, String bio,
                         String fotoUrl) {

    public static UsuarioDTO de(Usuario u) {
        return de(u, null);
    }

    public static UsuarioDTO de(Usuario u, String fotoUrl) {
        return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(), u.getCpf(), u.getTelefone(), u.getPapel(),
                u.getBio(), fotoUrl);
    }
}
