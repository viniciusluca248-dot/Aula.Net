package br.uema.aulanet.dto;

import br.uema.aulanet.model.Modalidade;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** RF04: disciplina, modalidade, valor/hora e percentual de comissão. */
public record OfertaRequest(
        @NotNull Integer disciplinaId,
        @NotNull Modalidade modalidade,
        @NotNull @DecimalMin(value = "1.00") @Digits(integer = 8, fraction = 2) BigDecimal valorHora,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal percentualComissao) {
}
