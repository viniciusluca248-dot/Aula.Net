package br.uema.aulanet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Parâmetros configuráveis da plataforma (application.properties, prefixo "aulanet"). */
@ConfigurationProperties(prefix = "aulanet")
public record AulanetProperties(Jwt jwt, Financeiro financeiro) {

    public record Jwt(String secret, long expiracaoHoras) {
    }

    /**
     * @param taxaJurosMensal        juros compostos ao mês aplicados no parcelamento (ex.: 0.0199 = 1,99%)
     * @param maxParcelas            quantidade máxima de parcelas
     * @param multaPercentual        multa única por atraso (ex.: 0.02 = 2%)
     * @param moraDiariaPercentual   juros de mora ao dia sobre a parcela em atraso (ex.: 0.00033 = 0,033%)
     * @param comissaoPadrao         comissão da plataforma sugerida ao professor (%)
     * @param diasPrimeiroVencimento dias entre a conclusão da aula e o 1º vencimento
     */
    public record Financeiro(BigDecimal taxaJurosMensal,
                             int maxParcelas,
                             BigDecimal multaPercentual,
                             BigDecimal moraDiariaPercentual,
                             BigDecimal comissaoPadrao,
                             int diasPrimeiroVencimento) {
    }
}
