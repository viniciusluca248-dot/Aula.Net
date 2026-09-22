package br.uema.aulanet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(@NotBlank @Size(max = 100) String nome,
                            @Size(max = 20) String telefone,
                            @Size(max = 1000) String bio) {
}
