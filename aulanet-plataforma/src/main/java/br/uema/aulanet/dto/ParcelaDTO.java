package br.uema.aulanet.dto;

import br.uema.aulanet.model.Parcela;
import br.uema.aulanet.model.StatusParcela;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaDTO(Integer id, Integer numeroParcela, BigDecimal valor, BigDecimal valorJuros, BigDecimal valorMulta,
                         BigDecimal valorAPagar, LocalDate dataVencimento, StatusParcela status) {

    public static ParcelaDTO de(Parcela p) {
        return new ParcelaDTO(p.getId(), p.getNumeroParcela(), p.getValor(), p.getValorJuros(), p.getValorMulta(),
                p.getValorAPagar(), p.getDataVencimento(), p.getStatus());
    }
}
