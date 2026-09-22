package br.uema.aulanet.service;

import br.uema.aulanet.config.AulanetProperties;
import br.uema.aulanet.dto.SimulacaoDTO;
import br.uema.aulanet.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraFinanceiraTest {

    private final CalculadoraFinanceira calc = new CalculadoraFinanceira(new AulanetProperties(
            new AulanetProperties.Jwt("x", 8),
            new AulanetProperties.Financeiro(new BigDecimal("0.0199"), 12, new BigDecimal("0.02"),
                    new BigDecimal("0.00033"), new BigDecimal("10.00"), 7)));

    @Test
    void aVistaNaoTemJuros() {
        SimulacaoDTO s = calc.simular(new BigDecimal("150.00"), 1);
        assertThat(s.totalJuros()).isEqualByComparingTo("0");
        assertThat(s.parcelas()).hasSize(1);
        assertThat(s.parcelas().get(0).valor()).isEqualByComparingTo("150.00");
    }

    @Test
    void parceladoAplicaTabelaPrice() {
        // PMT = 300 * 0,0199 / (1 - 1,0199^-3) = 104,01
        SimulacaoDTO s = calc.simular(new BigDecimal("300.00"), 3);
        assertThat(s.valorParcela()).isEqualByComparingTo("104.01");
        assertThat(s.parcelas()).hasSize(3);
        assertThat(s.parcelas().get(0).juros()).isEqualByComparingTo("5.97"); // 300 × 1,99%
        assertThat(s.parcelas().get(2).saldo()).isEqualByComparingTo("0");
        BigDecimal soma = s.parcelas().stream().map(SimulacaoDTO.Linha::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo(s.totalComJuros());
        assertThat(s.totalComJuros().subtract(s.totalJuros())).isEqualByComparingTo("300.00");
    }

    @Test
    void amortizacaoFechaOValorOriginalMesmoComDizimas() {
        SimulacaoDTO s = calc.simular(new BigDecimal("100.00"), 12);
        BigDecimal amortizado = s.parcelas().stream().map(SimulacaoDTO.Linha::amortizacao).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(amortizado).isEqualByComparingTo("100.00");
        assertThat(s.totalJuros()).isPositive();
    }

    @Test
    void rejeitaQuantidadeDeParcelasInvalida() {
        assertThatThrownBy(() -> calc.simular(new BigDecimal("100"), 13)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> calc.simular(new BigDecimal("100"), 0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> calc.simular(BigDecimal.ZERO, 2)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void encargosDeAtrasoSomamMultaEMora() {
        // 2% de 100 = 2,00 + 0,033% × 10 dias × 100 = 0,33
        assertThat(calc.calcularEncargosAtraso(new BigDecimal("100.00"), 10)).isEqualByComparingTo("2.33");
        assertThat(calc.calcularEncargosAtraso(new BigDecimal("100.00"), 0)).isEqualByComparingTo("0");
    }

    @Test
    void repasseDescontaComissao() {
        BigDecimal[] r = calc.calcularRepasse(new BigDecimal("200.00"), new BigDecimal("12.50"));
        assertThat(r[0]).isEqualByComparingTo("25.00");
        assertThat(r[1]).isEqualByComparingTo("175.00");
    }
}
