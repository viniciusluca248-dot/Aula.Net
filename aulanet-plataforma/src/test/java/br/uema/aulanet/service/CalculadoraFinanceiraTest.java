package br.uema.aulanet.service;

import br.uema.aulanet.config.AulanetProperties;
import br.uema.aulanet.dto.SimulacaoDTO;
import br.uema.aulanet.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Valores de referência da seção 2.6 e do Capítulo 6 da monografia. */
class CalculadoraFinanceiraTest {

    private final CalculadoraFinanceira calculadora = calculadora(1);

    private static CalculadoraFinanceira calculadora(int parcelasSemJuros) {
        return new CalculadoraFinanceira(new AulanetProperties(new AulanetProperties.Jwt("x", 8),
                new AulanetProperties.Financeiro(new BigDecimal("0.02"), parcelasSemJuros, 12,
                        new BigDecimal("0.02"), new BigDecimal("0.01"), new BigDecimal("10.00"), 7)));
    }

    @Test
    void deveCalcularParcelasPeloSistemaPrice() {
        // Tabela 1: R$ 600,00 em 3x a 2% a.m.
        List<LinhaAmortizacao> linhas = calculadora.tabelaPrice(new BigDecimal("600.00"), new BigDecimal("0.02"), 3);

        assertThat(linhas.get(0).valorParcela()).isEqualByComparingTo("208.05");
        assertThat(linhas.get(0).juros()).isEqualByComparingTo("12.00");
        assertThat(linhas.get(0).saldoDevedor()).isEqualByComparingTo("403.95");
        assertThat(linhas.get(1).valorParcela()).isEqualByComparingTo("208.05");
        assertThat(linhas.get(1).juros()).isEqualByComparingTo("8.08");
        assertThat(linhas.get(2).valorParcela()).isEqualByComparingTo("208.06");
        assertThat(linhas.get(2).juros()).isEqualByComparingTo("4.08");
        assertThat(linhas.get(2).saldoDevedor()).isEqualByComparingTo("0.00");
    }

    @Test
    void simulacaoInformaTaxaEfetivaAnualETotais() {
        SimulacaoDTO s = calculadora.simular(new BigDecimal("600.00"), 3);

        assertThat(s.taxaJurosMensal()).isEqualByComparingTo("0.02");
        assertThat(s.taxaEfetivaAnual()).isEqualByComparingTo("0.2682");   // 26,82% a.a.
        assertThat(s.valorAVista()).isEqualByComparingTo("600.00");
        assertThat(s.totalComJuros()).isEqualByComparingTo("624.16");
        assertThat(s.totalJuros()).isEqualByComparingTo("24.16");
    }

    @Test
    void parcelamentoEmSeisVezesDoCapitulo6() {
        // R$ 300,00 em 6x a 2% a.m. → R$ 21,35 de juros
        assertThat(calculadora.simular(new BigDecimal("300.00"), 6).totalJuros()).isEqualByComparingTo("21.35");
    }

    @Test
    void aVistaNaoTemJuros() {
        SimulacaoDTO s = calculadora.simular(new BigDecimal("150.00"), 1);
        assertThat(s.totalJuros()).isEqualByComparingTo("0");
        assertThat(s.parcelas()).singleElement().satisfies(l -> assertThat(l.valorParcela()).isEqualByComparingTo("150.00"));
    }

    @Test
    void carenciaDeJurosPermiteParcelarSemAcrescimo() {
        CalculadoraFinanceira tresSemJuros = calculadora(3);
        SimulacaoDTO s = tresSemJuros.simular(new BigDecimal("100.00"), 3);
        assertThat(s.taxaJurosMensal()).isEqualByComparingTo("0");
        assertThat(s.totalJuros()).isEqualByComparingTo("0");
        assertThat(s.parcelas().get(2).valorParcela()).isEqualByComparingTo("33.34");  // última absorve o centavo
        // acima da carência, a Tabela Price volta a ser aplicada
        assertThat(tresSemJuros.simular(new BigDecimal("100.00"), 4).totalJuros()).isPositive();
    }

    @Test
    void amortizacaoFechaOValorOriginal() {
        SimulacaoDTO s = calculadora.simular(new BigDecimal("100.00"), 12);
        BigDecimal amortizado = s.parcelas().stream().map(LinhaAmortizacao::amortizacao).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(amortizado).isEqualByComparingTo("100.00");
    }

    @Test
    void rejeitaQuantidadeDeParcelasInvalida() {
        assertThatThrownBy(() -> calculadora.simular(new BigDecimal("100"), 13)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> calculadora.simular(new BigDecimal("100"), 0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> calculadora.simular(BigDecimal.ZERO, 2)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void multaLimitadaA2PorCentoEJurosDeMoraProRataDie() {
        assertThat(calculadora.calcularMulta(new BigDecimal("100.00"))).isEqualByComparingTo("2.00");
        assertThat(calculadora.calcularJurosMora(new BigDecimal("100.00"), 30)).isEqualByComparingTo("1.00");
        assertThat(calculadora.calcularJurosMora(new BigDecimal("100.00"), 0)).isEqualByComparingTo("0");
    }
}
