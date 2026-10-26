package br.uema.aulanet.security;

import br.uema.aulanet.model.Papel;
import br.uema.aulanet.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Principal do Spring Security: guarda id e papel para evitar consultas extras nos controllers. */
public class UsuarioLogado implements UserDetails {

    private final Integer id;
    private final String email;
    private final String senha;
    private final String nome;
    private final Papel papel;

    public UsuarioLogado(Usuario u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.senha = u.getSenha();
        this.nome = u.getNome();
        this.papel = u.getPapel();
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public Papel getPapel() { return papel; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() { return senha; }

    @Override
    public String getUsername() { return email; }
}
