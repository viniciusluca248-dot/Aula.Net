package br.uema.aulanet.controller;

import br.uema.aulanet.dto.AgendamentoDTO;
import br.uema.aulanet.dto.AgendamentoRequest;
import br.uema.aulanet.dto.HistoricoDTO;
import br.uema.aulanet.security.UsuarioLogado;
import br.uema.aulanet.service.AgendamentoService;
import br.uema.aulanet.service.RelatorioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final RelatorioService relatorioService;

    public AgendamentoController(AgendamentoService agendamentoService, RelatorioService relatorioService) {
        this.agendamentoService = agendamentoService;
        this.relatorioService = relatorioService;
    }

    // ---------- ALUNO ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALUNO')")
    public AgendamentoDTO solicitar(@AuthenticationPrincipal UsuarioLogado u, @Valid @RequestBody AgendamentoRequest req) {
        return agendamentoService.solicitar(u.getId(), req);
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('ALUNO')")
    public List<AgendamentoDTO> meus(@AuthenticationPrincipal UsuarioLogado u) {
        return agendamentoService.doAluno(u.getId());
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ALUNO')")
    public AgendamentoDTO cancelar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        return agendamentoService.cancelarPeloAluno(u.getId(), id);
    }

    // ---------- PROFESSOR ----------

    @GetMapping("/professor")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<AgendamentoDTO> doProfessor(@AuthenticationPrincipal UsuarioLogado u) {
        return agendamentoService.doProfessor(u.getId());
    }

    @GetMapping("/professor/semana")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<AgendamentoDTO> semana(@AuthenticationPrincipal UsuarioLogado u,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agendamentoService.semanaDoProfessor(u.getId(), data);
    }

    @GetMapping("/professor/historico")
    @PreAuthorize("hasRole('PROFESSOR')")
    public HistoricoDTO historico(@AuthenticationPrincipal UsuarioLogado u,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.historicoProfessor(u.getId(), inicio, fim);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AgendamentoDTO confirmar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        return agendamentoService.confirmar(u.getId(), id);
    }

    @PatchMapping("/{id}/recusar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AgendamentoDTO recusar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        return agendamentoService.recusar(u.getId(), id);
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AgendamentoDTO concluir(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Integer id) {
        return agendamentoService.concluir(u.getId(), id);
    }
}
