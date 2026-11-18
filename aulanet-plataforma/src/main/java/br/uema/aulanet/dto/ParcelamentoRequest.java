package br.uema.aulanet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ParcelamentoRequest(@NotNull @Min(1) Integer qtdParcelas) {
}
