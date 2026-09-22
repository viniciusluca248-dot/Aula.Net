package br.uema.aulanet.dto;

import java.math.BigDecimal;
import java.util.List;

/** Pré-visualização do parcelamento exibida em tempo real na tela de pagamento. */
public record SimulacaoDTO(BigDecimal valorOriginal, int qtdParcelas, BigDecimal taxaJurosMensal,
                           BigDecimal valorParcela, BigDecimal totalComJuros, BigDecimal totalJuros,
                           List<Linha> parcelas) {

    public record Linha(int numero, BigDecimal valor, BigDecimal juros, BigDecimal amortizacao, BigDecimal saldo) {
    }
}
