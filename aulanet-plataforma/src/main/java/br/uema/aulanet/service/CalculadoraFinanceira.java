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
 * Cálculos financeiros puros (sem acesso a banco), sempre com BigDecimal.
 * <ul>
 *   <li>Parcelamento: juros compostos pela Tabela Price — PMT = PV · i / (1 − (1 + i)^−n).</li>
 *   <li>Atraso: multa única + juros de mora diários sobre o valor da parcela.</li>
 *   <li>Repasse: valor bruto − comissão (%) da plataforma.</li>
 * </ul>
 */
@Component
public class CalculadoraFinanceira {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final MathContext MC = MathContext.DECIMAL64;

    private final AulanetProperties.Financeiro cfg;

    public CalculadoraFinanceira(AulanetProperties props) {
        this.cfg = props.financeiro();
    }

    public SimulacaoDTO simular(BigDecimal valor, int qtdParcelas) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("Valor deve ser maior que zero.");
        }
        if (qtdParcelas < 1 || qtdParcelas > cfg.maxParcelas()) {
            throw new RegraNegocioException("Quantidade de parcelas deve estar entre 1 e " + cfg.maxParcelas() + ".");
        }
        BigDecimal pv = valor.setScale(2, RoundingMode.HALF_EVEN);
        // à vista não tem juros
        BigDecimal i = qtdParcelas == 1 ? BigDecimal.ZERO : cfg.taxaJurosMensal();

        BigDecimal pmt;
        if (i.signum() == 0) {
            pmt = pv.divide(BigDecimal.valueOf(qtdParcelas), 2, RoundingMode.HALF_EVEN);
        } else {
            BigDecimal fator = BigDecimal.ONE.add(i).pow(-qtdParcelas, MC);
            pmt = pv.multiply(i, MC).divide(BigDecimal.ONE.subtract(fator), MC).setScale(2, RoundingMode.HALF_EVEN);
        }

        List<SimulacaoDTO.Linha> linhas = new ArrayList<>();
        BigDecimal saldo = pv;
        BigDecimal total = BigDecimal.ZERO;
        for (int k = 1; k <= qtdParcelas; k++) {
            BigDecimal juros = saldo.multiply(i).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal amortizacao;
            BigDecimal parcela;
            if (k == qtdParcelas) {
                // última parcela absorve as diferenças de arredondamento e zera o saldo
                amortizacao = saldo;
                parcela = saldo.add(juros);
            } else {
                parcela = pmt;
                amortizacao = pmt.subtract(juros);
            }
            saldo = saldo.subtract(amortizacao);
            total = total.add(parcela);
            linhas.add(new SimulacaoDTO.Linha(k, parcela, juros, amortizacao, saldo));
        }
        return new SimulacaoDTO(pv, qtdParcelas, i, pmt, total, total.subtract(pv), linhas);
    }

    /** Multa (percentual fixo) + mora diária proporcional aos dias em atraso. */
    public BigDecimal calcularEncargosAtraso(BigDecimal valorParcela, long diasAtraso) {
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal multa = valorParcela.multiply(cfg.multaPercentual());
        BigDecimal mora = valorParcela.multiply(cfg.moraDiariaPercentual()).multiply(BigDecimal.valueOf(diasAtraso));
        return multa.add(mora).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** @return [comissão, líquido] */
    public BigDecimal[] calcularRepasse(BigDecimal valorBruto, BigDecimal percentualComissao) {
        BigDecimal comissao = valorBruto.multiply(percentualComissao).divide(CEM, 2, RoundingMode.HALF_EVEN);
        return new BigDecimal[]{comissao, valorBruto.subtract(comissao)};
    }

    public int maxParcelas() {
        return cfg.maxParcelas();
    }

    public int diasPrimeiroVencimento() {
        return cfg.diasPrimeiroVencimento();
    }
}
