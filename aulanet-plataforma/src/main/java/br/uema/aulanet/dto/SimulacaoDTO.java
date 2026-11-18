package br.uema.aulanet.dto;

import br.uema.aulanet.service.LinhaAmortizacao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simulação exibida antes da confirmação do pagamento (RF11), com as informações exigidas pelo art. 52 do CDC:
 * valor de cada prestação, taxa mensal e efetiva anual, total a pagar com e sem financiamento.
 */
public record SimulacaoDTO(BigDecimal valorAVista, int qtdParcelas, BigDecimal taxaJurosMensal,
                           BigDecimal taxaEfetivaAnual, BigDecimal valorParcela, BigDecimal totalComJuros,
                           BigDecimal totalJuros, List<LinhaAmortizacao> parcelas) {
}
