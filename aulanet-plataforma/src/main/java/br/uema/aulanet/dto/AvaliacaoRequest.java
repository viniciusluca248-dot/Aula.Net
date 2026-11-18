package br.uema.aulanet.dto;

import jakarta.validation.constraints.*;

/** RF16: nota de 1 a 5 e comentário. */
public record AvaliacaoRequest(@NotNull Integer agendamentoId,
                               @NotNull @Min(1) @Max(5) Integer nota,
                               @Size(max = 2000) String comentario) {
}
