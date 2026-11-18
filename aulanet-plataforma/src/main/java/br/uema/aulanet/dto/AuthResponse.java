package br.uema.aulanet.dto;

import java.time.Instant;

public record AuthResponse(String token, Instant expiraEm, UsuarioDTO usuario) {
}
