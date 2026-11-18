package br.uema.aulanet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplinaRequest(@NotBlank @Size(max = 100) String nome) {
}
