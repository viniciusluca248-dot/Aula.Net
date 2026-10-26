package br.uema.aulanet.security;

import br.uema.aulanet.config.AulanetProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Geração e validação do token JWT (RF02 / RNF04: expiração de 8 horas). */
@Service
public class JwtService {

    private final SecretKey chave;
    private final Duration validade;

    public JwtService(AulanetProperties props) {
        this.chave = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.validade = Duration.ofHours(props.jwt().expiracaoHoras());
    }

    public String gerarToken(UsuarioLogado usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("id", usuario.getId())
                .claim("papel", usuario.getPapel().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    public Instant expiracao(String token) {
        return extrairClaims(token).map(c -> c.getExpiration().toInstant()).orElse(null);
    }

    /** Retorna o e-mail (subject) se o token for válido e não estiver expirado. */
    public Optional<String> emailValido(String token) {
        return extrairClaims(token).map(Claims::getSubject);
    }

    private Optional<Claims> extrairClaims(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
