package br.uema.aulanet.service;

import br.uema.aulanet.dto.AvaliacaoDTO;
import br.uema.aulanet.dto.AvaliacaoRequest;
import br.uema.aulanet.exception.AcessoNegadoException;
import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.Agendamento;
import br.uema.aulanet.model.Avaliacao;
import br.uema.aulanet.model.StatusAgendamento;
import br.uema.aulanet.repository.AgendamentoRepository;
import br.uema.aulanet.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF16: avaliação do professor após aula CONCLUIDA (uma por agendamento). */
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, AgendamentoRepository agendamentoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public AvaliacaoDTO avaliar(Integer alunoId, AvaliacaoRequest req) {
        Agendamento ag = agendamentoRepository.findById(req.agendamentoId())
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado."));
        if (!ag.getAluno().getId().equals(alunoId)) {
            throw new AcessoNegadoException("somente o aluno da aula pode avaliá-la.");
        }
        if (ag.getStatus() != StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Só é possível avaliar aulas concluídas.");
        }
        if (avaliacaoRepository.existsByAgendamentoId(ag.getId())) {
            throw new RegraNegocioException("Esta aula já foi avaliada.");
        }
        Avaliacao a = new Avaliacao();
        a.setAgendamento(ag);
        a.setAluno(ag.getAluno());
        a.setProfessor(ag.getOferta().getProfessor());
        a.setNota(req.nota());
        a.setComentario(req.comentario());
        return AvaliacaoDTO.de(avaliacaoRepository.save(a));
    }
}
