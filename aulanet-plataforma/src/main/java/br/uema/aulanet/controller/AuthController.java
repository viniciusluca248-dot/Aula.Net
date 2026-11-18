package br.uema.aulanet.controller;

import br.uema.aulanet.dto.*;
import br.uema.aulanet.model.FotoUsuario;
import br.uema.aulanet.security.UsuarioLogado;
import br.uema.aulanet.service.AuthService;
import br.uema.aulanet.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final PerfilService perfilService;

    public AuthController(AuthService authService, PerfilService perfilService) {
        this.authService = authService;
        this.perfilService = perfilService;
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
    public MeuPerfilDTO eu(@AuthenticationPrincipal UsuarioLogado usuario) {
        return perfilService.meuPerfil(usuario.getId());
    }

    @PutMapping("/usuarios/me")
    public MeuPerfilDTO atualizarPerfil(@AuthenticationPrincipal UsuarioLogado usuario, @Valid @RequestBody PerfilRequest req) {
        return perfilService.atualizar(usuario.getId(), req);
    }

    @PutMapping(value = "/usuarios/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeuPerfilDTO enviarFoto(@AuthenticationPrincipal UsuarioLogado usuario,
                                   @RequestParam("foto") MultipartFile foto) throws IOException {
        return perfilService.salvarFoto(usuario.getId(), foto.getBytes());
    }

    @DeleteMapping("/usuarios/me/foto")
    public MeuPerfilDTO removerFoto(@AuthenticationPrincipal UsuarioLogado usuario) {
        return perfilService.removerFoto(usuario.getId());
    }

    /** Foto pública do perfil. A URL leva a versão (?v=), então pode ficar em cache por bastante tempo. */
    @GetMapping("/usuarios/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Integer id) {
        FotoUsuario f = perfilService.foto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(f.getConteudo());
    }
}
