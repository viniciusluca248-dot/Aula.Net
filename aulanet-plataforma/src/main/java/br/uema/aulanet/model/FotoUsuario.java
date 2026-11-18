package br.uema.aulanet.model;

import jakarta.persistence.*;

/** Foto de perfil (JPEG, PNG ou WebP, até 2 MB), guardada no banco para funcionar igual no H2 e no MySQL. */
@Entity
@Table(name = "foto_usuario")
public class FotoUsuario {

    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Lob
    @Column(name = "conteudo", nullable = false, length = 3_000_000)
    private byte[] conteudo;

    public FotoUsuario() {
    }

    public FotoUsuario(Integer id, String tipo, byte[] conteudo) {
        this.id = id;
        this.tipo = tipo;
        this.conteudo = conteudo;
    }

    public Integer getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public byte[] getConteudo() { return conteudo; }
    public void setConteudo(byte[] conteudo) { this.conteudo = conteudo; }
}
