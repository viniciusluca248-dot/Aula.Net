package br.uema.aulanet.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** RF07: professor (via oferta, que já define a disciplina), data, horário e duração. */
public record AgendamentoRequest(
        @NotNull Integer ofertaId,
        @NotNull @FutureOrPresent LocalDate dataAula,
        @NotNull LocalTime horarioInicio,
        @NotNull @Min(30) @Max(240) Integer duracaoMinutos) {
}
