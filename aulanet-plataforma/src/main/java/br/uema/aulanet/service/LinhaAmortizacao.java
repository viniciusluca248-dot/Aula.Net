package br.uema.aulanet.service;

import java.math.BigDecimal;

/** Uma linha da tabela de amortização: prestação decomposta em juros e amortização, e o saldo que resta. */
public record LinhaAmortizacao(int numero, BigDecimal valorParcela, BigDecimal juros, BigDecimal amortizacao,
                               BigDecimal saldoDevedor) {
}
