package br.uema.aulanet.dto;

import br.uema.aulanet.model.Papel;
import jakarta.validation.constraints.*;

/** RF01: cadastro com nome, e-mail, senha, CPF e papel. */
public record CadastroRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 72, message = "deve ter entre 6 e 72 caracteres") String senha,
        @Pattern(regexp = "^$|^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$", message = "CPF inválido") String cpf,
        @Size(max = 20) String telefone,
        @NotNull Papel papel,
        @Size(max = 1000) String bio) {
}
