package br.uema.aulanet.model;

import jakarta.persistence.*;

/**
 * Dados complementares do perfil (1 : 1 com usuario): cidade, formação, experiência, link do currículo e a
 * versão da foto. A foto em si fica em foto_usuario, para não ser carregada nas listagens.
 */
@Entity
@Table(name = "perfil_usuario", indexes = @Index(name = "idx_perfil_cidade", columnList = "cidade"))
public class PerfilUsuario {

    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 2000)
    private String formacao;

    @Column(length = 2000)
    private String experiencia;

    @Column(name = "link_curriculo", length = 255)
    private String linkCurriculo;

    /** Momento do último envio da foto (epoch ms); nulo quando não há foto. Usado na URL para evitar cache antigo. */
    @Column(name = "foto_versao")
    private Long fotoVersao;

    public PerfilUsuario() {
    }

    public PerfilUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getFormacao() { return formacao; }
    public void setFormacao(String formacao) { this.formacao = formacao; }
    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String experiencia) { this.experiencia = experiencia; }
    public String getLinkCurriculo() { return linkCurriculo; }
    public void setLinkCurriculo(String linkCurriculo) { this.linkCurriculo = linkCurriculo; }
    public Long getFotoVersao() { return fotoVersao; }
    public void setFotoVersao(Long fotoVersao) { this.fotoVersao = fotoVersao; }
}
