package br.uema.aulanet.controller;

import br.uema.aulanet.dto.*;
import br.uema.aulanet.security.UsuarioLogado;
import br.uema.aulanet.service.AuthService;
import br.uema.aulanet.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrar(req);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/usuarios/me")
    public UsuarioDTO eu(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuarioService.buscar(usuario.getId());
    }

    @PutMapping("/usuarios/me")
    public UsuarioDTO atualizarPerfil(@AuthenticationPrincipal UsuarioLogado usuario, @Valid @RequestBody PerfilRequest req) {
        return usuarioService.atualizarPerfil(usuario.getId(), req);
    }
}
