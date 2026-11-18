package br.uema.aulanet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Edição do próprio perfil. Formação, experiência e link do currículo aparecem no perfil público do professor. */
public record PerfilRequest(@NotBlank @Size(max = 100) String nome,
                            @Size(max = 20) String telefone,
                            @Size(max = 1000) String bio,
                            @Size(max = 100) String cidade,
                            @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve ter 2 letras") String uf,
                            @Size(max = 2000) String formacao,
                            @Size(max = 2000) String experiencia,
                            @Size(max = 255)
                            @Pattern(regexp = "^$|^https?://\\S+$", message = "deve começar com http:// ou https://")
                            String linkCurriculo) {
}
