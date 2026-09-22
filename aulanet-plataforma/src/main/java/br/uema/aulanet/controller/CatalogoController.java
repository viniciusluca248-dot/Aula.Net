package br.uema.aulanet.controller;

import br.uema.aulanet.dto.OfertaDTO;
import br.uema.aulanet.dto.ProfessorPerfilDTO;
import br.uema.aulanet.model.Disciplina;
import br.uema.aulanet.model.Modalidade;
import br.uema.aulanet.service.DisciplinaService;
import br.uema.aulanet.service.OfertaService;
import br.uema.aulanet.service.ProfessorService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Endpoints públicos de busca e descoberta (RF05 / RF06). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final DisciplinaService disciplinaService;
    private final OfertaService ofertaService;
    private final ProfessorService professorService;

    public CatalogoController(DisciplinaService disciplinaService, OfertaService ofertaService,
                              ProfessorService professorService) {
        this.disciplinaService = disciplinaService;
        this.ofertaService = ofertaService;
        this.professorService = professorService;
    }

    @GetMapping("/disciplinas")
    public List<Disciplina> disciplinas() {
        return disciplinaService.listar();
    }

    @GetMapping("/ofertas")
    public List<OfertaDTO> buscar(@RequestParam(required = false) Integer disciplinaId,
                                  @RequestParam(required = false) Modalidade modalidade,
                                  @RequestParam(required = false) BigDecimal precoMin,
                                  @RequestParam(required = false) BigDecimal precoMax,
                                  @RequestParam(required = false) String termo) {
        return ofertaService.buscar(disciplinaId, modalidade, precoMin, precoMax, termo);
    }

    @GetMapping("/professores/{id}")
    public ProfessorPerfilDTO perfil(@PathVariable Integer id) {
        return professorService.perfil(id);
    }
}
