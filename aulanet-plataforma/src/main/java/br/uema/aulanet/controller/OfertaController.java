package br.uema.aulanet.controller;

import br.uema.aulanet.dto.OfertaDTO;
import br.uema.aulanet.dto.OfertaRequest;
import br.uema.aulanet.security.UsuarioLogado;
import br.uema.aulanet.service.OfertaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF04: professor gerencia os serviços (ofertas de aula) que presta. */
@RestController
@RequestMapping("/api/ofertas")
@PreAuthorize("hasRole('PROFESSOR')")
public class OfertaController {

    private final OfertaService ofertaService;

    public OfertaController(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @GetMapping("/minhas")
    public List<OfertaDTO> minhas(@AuthenticationPrincipal UsuarioLogado u) {
        return ofertaService.listarDoProfessor(u.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfertaDTO criar(@AuthenticationPrincipal UsuarioLogado u, @Valid @RequestBody OfertaRequest req) {
        return ofertaService.criar(u.getId(), req);
    }

    @PutMapping("/{id}")
    public OfertaDTO atualizar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id,
                               @Valid @RequestBody OfertaRequest req) {
        return ofertaService.atualizar(u.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        ofertaService.excluir(u.getId(), id);
    }
}
