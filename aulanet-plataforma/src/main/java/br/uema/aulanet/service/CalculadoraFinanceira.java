package br.uema.aulanet.service;

import br.uema.aulanet.config.AulanetProperties;
import br.uema.aulanet.dto.SimulacaoDTO;
import br.uema.aulanet.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Matemática financeira pura (sem acesso a banco), sempre com BigDecimal e arredondamento a centavos.
 * <ul>
 *   <li>Parcelamento: Tabela Price — PMT = PV · [i(1 + i)^n] / [(1 + i)^n − 1].</li>
 *   <li>Atraso: multa única + juros de mora proporcionais aos dias (pro rata die).</li>
 * </ul>
 */
@Component
public class CalculadoraFinanceira {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final BigDecimal TRINTA = BigDecimal.valueOf(30);

    private final AulanetProperties.Financeiro cfg;

    public CalculadoraFinanceira(AulanetProperties props) {
        this.cfg = props.financeiro();
    }

    /** Tabela Price: prestações iguais; a última absorve a diferença de centavos e zera o saldo. */
    public List<LinhaAmortizacao> tabelaPrice(BigDecimal principal, BigDecimal taxaMensal, int n) {
        if (n < 1) throw new RegraNegocioException("Número de parcelas inválido");

        BigDecimal prestacao;
        if (taxaMensal.signum() == 0) {
            prestacao = principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        } else {
            // PMT = PV * [i(1+i)^n] / [(1+i)^n - 1]
            BigDecimal fator = BigDecimal.ONE.add(taxaMensal).pow(n, MC);
            prestacao = principal.multiply(taxaMensal.multiply(fator, MC), MC)
                    .divide(fator.subtract(BigDecimal.ONE), MC)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        List<LinhaAmortizacao> linhas = new ArrayList<>();
        BigDecimal saldo = principal;
        for (int k = 1; k <= n; k++) {
            BigDecimal juros = saldo.multiply(taxaMensal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amortizacao = prestacao.subtract(juros);
            BigDecimal valorParcela = prestacao;
            if (k == n) {                       // última parcela absorve a diferença de centavos
                amortizacao = saldo;
                valorParcela = saldo.add(juros);
            }
            saldo = saldo.subtract(amortizacao);
            linhas.add(new LinhaAmortizacao(k, valorParcela, juros, amortizacao, saldo));
        }
        return linhas;
    }

    /** Simulação do parcelamento (RF11), aplicando a carência de juros configurada. */
    public SimulacaoDTO simular(BigDecimal valor, int qtdParcelas) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("Valor deve ser maior que zero.");
        }
        if (qtdParcelas < 1 || qtdParcelas > cfg.maxParcelas()) {
            throw new RegraNegocioException("Quantidade de parcelas deve estar entre 1 e " + cfg.maxParcelas() + ".");
        }
        BigDecimal principal = valor.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxa = taxaPara(qtdParcelas);
        List<LinhaAmortizacao> linhas = tabelaPrice(principal, taxa, qtdParcelas);
        BigDecimal total = linhas.stream().map(LinhaAmortizacao::valorParcela).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SimulacaoDTO(principal, qtdParcelas, taxa, taxaEfetivaAnual(taxa),
                linhas.get(0).valorParcela(), total, total.subtract(principal), linhas);
    }

    /** Taxa mensal aplicada a n parcelas: zero dentro da carência de juros. */
    public BigDecimal taxaPara(int qtdParcelas) {
        return qtdParcelas <= cfg.parcelasSemJuros() ? BigDecimal.ZERO : cfg.taxaJurosMensal();
    }

    /** Taxa efetiva anual equivalente: (1 + i)^12 − 1, em fração com 4 casas (0,2682 = 26,82%). */
    public BigDecimal taxaEfetivaAnual(BigDecimal taxaMensal) {
        return BigDecimal.ONE.add(taxaMensal).pow(12, MC).subtract(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
    }

    /** Multa moratória, aplicada uma única vez quando a parcela entra em atraso. */
    public BigDecimal calcularMulta(BigDecimal valorParcela) {
        return valorParcela.multiply(cfg.percentualMulta()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Juros de mora simples, proporcionais aos dias de atraso: valor × taxa mensal × dias / 30. */
    public BigDecimal calcularJurosMora(BigDecimal valorParcela, long diasAtraso) {
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return valorParcela.multiply(cfg.jurosMoraMensal())
                .multiply(BigDecimal.valueOf(diasAtraso))
                .divide(TRINTA, 2, RoundingMode.HALF_UP);
    }

    public int diasPrimeiroVencimento() {
        return cfg.diasPrimeiroVencimento();
    }
}
