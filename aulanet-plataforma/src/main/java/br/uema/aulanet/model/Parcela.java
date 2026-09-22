package br.uema.aulanet.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parcela", indexes = {
        @Index(name = "idx_parcela_status_venc", columnList = "status, data_vencimento")
})
public class Parcela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parcela")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pagamento", nullable = false)
    private Pagamento pagamento;

    @Column(name = "numero_parcela", nullable = false)
    private Integer numeroParcela;

    /** Valor da parcela já com juros de parcelamento (sem multa). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusParcela status = StatusParcela.PENDENTE;

    @Column(name = "valor_juros", precision = 10, scale = 2)
    private BigDecimal valorJuros = BigDecimal.ZERO;

    @Column(name = "valor_multa", precision = 10, scale = 2)
    private BigDecimal valorMulta = BigDecimal.ZERO;

    public BigDecimal getValorAPagar() {
        return valor.add(valorMulta == null ? BigDecimal.ZERO : valorMulta);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) { this.pagamento = pagamento; }
    public Integer getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(Integer numeroParcela) { this.numeroParcela = numeroParcela; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public StatusParcela getStatus() { return status; }
    public void setStatus(StatusParcela status) { this.status = status; }
    public BigDecimal getValorJuros() { return valorJuros; }
    public void setValorJuros(BigDecimal valorJuros) { this.valorJuros = valorJuros; }
    public BigDecimal getValorMulta() { return valorMulta; }
    public void setValorMulta(BigDecimal valorMulta) { this.valorMulta = valorMulta; }
}
