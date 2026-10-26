package br.uema.aulanet.controller;

import br.uema.aulanet.dto.*;
import br.uema.aulanet.model.Disciplina;
import br.uema.aulanet.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Funcionalidades exclusivas do ADMIN (RF03 / RF18). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RelatorioService relatorioService;
    private final UsuarioService usuarioService;
    private final FinanceiroService financeiroService;
    private final OfertaService ofertaService;
    private final DisciplinaService disciplinaService;

    public AdminController(RelatorioService relatorioService, UsuarioService usuarioService,
                           FinanceiroService financeiroService, OfertaService ofertaService,
                           DisciplinaService disciplinaService) {
        this.relatorioService = relatorioService;
        this.usuarioService = usuarioService;
        this.financeiroService = financeiroService;
        this.ofertaService = ofertaService;
        this.disciplinaService = disciplinaService;
    }

    @GetMapping("/relatorio")
    public RelatorioAdminDTO relatorio() {
        return relatorioService.relatorioAdmin();
    }

    @GetMapping("/usuarios")
    public List<UsuarioDTO> usuarios() {
        return usuarioService.listarTodos();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO criarUsuario(@Valid @RequestBody CadastroRequest req) {
        return usuarioService.criarPorAdmin(req);
    }

    @GetMapping("/repasses")
    public List<RepasseDTO> repasses() {
        return financeiroService.todosRepasses();
    }

    @PatchMapping("/repasses/{id}/realizar")
    public RepasseDTO realizarRepasse(@PathVariable Integer id) {
        return financeiroService.realizarRepasse(id);
    }

    /** Executa manualmente a mesma rotina da tarefa agendada diária de multas (RF13). */
    @PostMapping("/processar-atrasos")
    public Map<String, Integer> processarAtrasos() {
        return Map.of("parcelasAtualizadas", financeiroService.processarAtrasos());
    }

    @GetMapping("/ofertas")
    public List<OfertaDTO> ofertas() {
        return ofertaService.listarTodas();
    }

    @PatchMapping("/ofertas/{id}/comissao")
    public OfertaDTO alterarComissao(@PathVariable Integer id, @Valid @RequestBody ComissaoRequest req) {
        return ofertaService.alterarComissao(id, req.percentualComissao());
    }

    @PostMapping("/disciplinas")
    @ResponseStatus(HttpStatus.CREATED)
    public Disciplina criarDisciplina(@Valid @RequestBody DisciplinaRequest req) {
        return disciplinaService.criar(req.nome());
    }
}
