package br.uema.aulanet.service;

import br.uema.aulanet.dto.AgendamentoDTO;
import br.uema.aulanet.dto.AgendamentoRequest;
import br.uema.aulanet.exception.AcessoNegadoException;
import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.*;
import br.uema.aulanet.repository.AgendamentoRepository;
import br.uema.aulanet.repository.AvaliacaoRepository;
import br.uema.aulanet.repository.OfertaAulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Módulo de agendamento (RF07–RF10): SOLICITADO → CONFIRMADO → CONCLUIDO, ou CANCELADO.
 */
@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final OfertaAulaRepository ofertaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;
    private final FinanceiroService financeiroService;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentoRepository, OfertaAulaRepository ofertaRepository,
                              AvaliacaoRepository avaliacaoRepository, UsuarioService usuarioService,
                              FinanceiroService financeiroService, Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.ofertaRepository = ofertaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.usuarioService = usuarioService;
        this.financeiroService = financeiroService;
        this.clock = clock;
    }

    /** RF07: aluno solicita a aula. */
    @Transactional
    public AgendamentoDTO solicitar(Integer alunoId, AgendamentoRequest req) {
        OfertaAula oferta = ofertaRepository.findById(req.ofertaId())
                .orElseThrow(() -> new NaoEncontradoException("Oferta não encontrada."));
        if (LocalDateTime.of(req.dataAula(), req.horarioInicio()).isBefore(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("Não é possível agendar aulas no passado.");
        }
        Agendamento a = new Agendamento();
        a.setAluno(usuarioService.obter(alunoId));
        a.setOferta(oferta);
        a.setDataAula(req.dataAula());
        a.setHorarioInicio(req.horarioInicio());
        a.setDuracaoMinutos(req.duracaoMinutos());
        a.setStatus(StatusAgendamento.SOLICITADO);
        verificarConflito(a);
        return AgendamentoDTO.de(agendamentoRepository.save(a), false);
    }

    /** RF08: professor confirma. */
    @Transactional
    public AgendamentoDTO confirmar(Integer professorId, Integer agendamentoId) {
        Agendamento a = obterDoProfessor(professorId, agendamentoId);
        exigirStatus(a, StatusAgendamento.SOLICITADO);
        verificarConflito(a);
        a.setStatus(StatusAgendamento.CONFIRMADO);
        return AgendamentoDTO.de(a, false);
    }

    /** RF08: professor recusa (status CANCELADO). */
    @Transactional
    public AgendamentoDTO recusar(Integer professorId, Integer agendamentoId) {
        Agendamento a = obterDoProfessor(professorId, agendamentoId);
        exigirStatus(a, StatusAgendamento.SOLICITADO);
        a.setStatus(StatusAgendamento.CANCELADO);
        return AgendamentoDTO.de(a, false);
    }

    /** Aluno desiste de uma aula ainda não concluída. */
    @Transactional
    public AgendamentoDTO cancelarPeloAluno(Integer alunoId, Integer agendamentoId) {
        Agendamento a = obter(agendamentoId);
        if (!a.getAluno().getId().equals(alunoId)) {
            throw new AcessoNegadoException("o agendamento pertence a outro aluno.");
        }
        if (a.getStatus() != StatusAgendamento.SOLICITADO && a.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Somente aulas solicitadas ou confirmadas podem ser canceladas.");
        }
        a.setStatus(StatusAgendamento.CANCELADO);
        return AgendamentoDTO.de(a, false);
    }

    /** RF09: professor finaliza a aula e o pagamento é gerado automaticamente. */
    @Transactional
    public AgendamentoDTO concluir(Integer professorId, Integer agendamentoId) {
        Agendamento a = obterDoProfessor(professorId, agendamentoId);
        exigirStatus(a, StatusAgendamento.CONFIRMADO);
        if (LocalDateTime.of(a.getDataAula(), a.getHorarioInicio()).isAfter(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("A aula só pode ser concluída depois do horário de início.");
        }
        a.setStatus(StatusAgendamento.CONCLUIDO);
        financeiroService.gerarPagamento(a);
        return AgendamentoDTO.de(a, false);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoDTO> doAluno(Integer alunoId) {
        return agendamentoRepository.findByAluno(alunoId).stream()
                .map(a -> AgendamentoDTO.de(a, avaliacaoRepository.existsByAgendamentoId(a.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoDTO> doProfessor(Integer professorId) {
        return agendamentoRepository.findByProfessor(professorId).stream()
                .map(a -> AgendamentoDTO.de(a, false)).toList();
    }

    /** RF10: agendamentos confirmados da semana (segunda a domingo) que contém a data informada. */
    @Transactional(readOnly = true)
    public List<AgendamentoDTO> semanaDoProfessor(Integer professorId, LocalDate referencia) {
        LocalDate ref = referencia != null ? referencia : LocalDate.now(clock);
        LocalDate segunda = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return agendamentoRepository.findByProfessorPeriodo(professorId, segunda, segunda.plusDays(6),
                        StatusAgendamento.CONFIRMADO).stream()
                .map(a -> AgendamentoDTO.de(a, false)).toList();
    }

    /** Impede dois agendamentos CONFIRMADOS sobrepostos para o mesmo professor. */
    private void verificarConflito(Agendamento novo) {
        Integer professorId = novo.getOferta().getProfessor().getId();
        boolean conflito = agendamentoRepository
                .findByProfessorDataStatus(professorId, novo.getDataAula(), StatusAgendamento.CONFIRMADO).stream()
                .filter(a -> !a.getId().equals(novo.getId()))
                .anyMatch(a -> novo.getHorarioInicio().isBefore(a.getHorarioFim())
                        && a.getHorarioInicio().isBefore(novo.getHorarioFim()));
        if (conflito) {
            throw new RegraNegocioException("O professor já possui uma aula confirmada neste horário.");
        }
    }

    private void exigirStatus(Agendamento a, StatusAgendamento esperado) {
        if (a.getStatus() != esperado) {
            throw new RegraNegocioException("Operação inválida: o agendamento está " + a.getStatus()
                    + " (esperado " + esperado + ").");
        }
    }

    private Agendamento obter(Integer id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado."));
    }

    private Agendamento obterDoProfessor(Integer professorId, Integer agendamentoId) {
        Agendamento a = obter(agendamentoId);
        if (!a.getOferta().getProfessor().getId().equals(professorId)) {
            throw new AcessoNegadoException("o agendamento pertence a outro professor.");
        }
        return a;
    }
}
