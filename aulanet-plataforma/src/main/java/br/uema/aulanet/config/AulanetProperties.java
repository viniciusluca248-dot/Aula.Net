package br.uema.aulanet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Parâmetros configuráveis da plataforma (application.properties, prefixo "aulanet"). */
@ConfigurationProperties(prefix = "aulanet")
public record AulanetProperties(Jwt jwt, Financeiro financeiro) {

    public record Jwt(String secret, long expiracaoHoras) {
    }

    /**
     * @param taxaJurosMensal        juros compostos ao mês do parcelamento pela Tabela Price (ex.: 0.02 = 2%)
     * @param parcelasSemJuros       carência de juros: até esta quantidade de parcelas não há juros (1 = só à vista)
     * @param maxParcelas            quantidade máxima de parcelas
     * @param percentualMulta        multa moratória única, limitada a 2% pelo art. 52, § 1º, do CDC (ex.: 0.02)
     * @param jurosMoraMensal        juros de mora ao mês, cobrados pro rata die (ex.: 0.01 = 1% a.m.)
     * @param comissaoPadrao         comissão da plataforma sugerida ao professor (%)
     * @param diasPrimeiroVencimento dias entre a conclusão da aula e o 1º vencimento
     */
    public record Financeiro(BigDecimal taxaJurosMensal,
                             int parcelasSemJuros,
                             int maxParcelas,
                             BigDecimal percentualMulta,
                             BigDecimal jurosMoraMensal,
                             BigDecimal comissaoPadrao,
                             int diasPrimeiroVencimento) {
    }
}
