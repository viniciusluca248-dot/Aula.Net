package br.uema.aulanet.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Repasse ao professor (RF14): valor bruto menos a comissão da plataforma. */
@Entity
@Table(name = "repasse")
public class Repasse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repasse")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pagamento", nullable = false, unique = true)
    private Pagamento pagamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_professor", nullable = false)
    private Usuario professor;

    @Column(name = "valor_bruto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_comissao", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorComissao;

    @Column(name = "valor_liquido", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusRepasse status = StatusRepasse.PENDENTE;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) { this.pagamento = pagamento; }
    public Usuario getProfessor() { return professor; }
    public void setProfessor(Usuario professor) { this.professor = professor; }
    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) { this.valorBruto = valorBruto; }
    public BigDecimal getValorComissao() { return valorComissao; }
    public void setValorComissao(BigDecimal valorComissao) { this.valorComissao = valorComissao; }
    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) { this.valorLiquido = valorLiquido; }
    public StatusRepasse getStatus() { return status; }
    public void setStatus(StatusRepasse status) { this.status = status; }
}
