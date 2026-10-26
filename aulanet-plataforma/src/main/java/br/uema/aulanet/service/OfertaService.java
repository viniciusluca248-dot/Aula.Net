package br.uema.aulanet.service;

import br.uema.aulanet.config.AulanetProperties;
import br.uema.aulanet.dto.OfertaDTO;
import br.uema.aulanet.dto.OfertaRequest;
import br.uema.aulanet.exception.AcessoNegadoException;
import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.Modalidade;
import br.uema.aulanet.model.OfertaAula;
import br.uema.aulanet.repository.AvaliacaoRepository;
import br.uema.aulanet.repository.DisciplinaRepository;
import br.uema.aulanet.repository.OfertaAulaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** RF04 (cadastro de serviços) e RF06 (busca com filtros). */
@Service
public class OfertaService {

    private final OfertaAulaRepository ofertaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;
    private final AulanetProperties props;

    public OfertaService(OfertaAulaRepository ofertaRepository, DisciplinaRepository disciplinaRepository,
                         AvaliacaoRepository avaliacaoRepository, UsuarioService usuarioService,
                         AulanetProperties props) {
        this.ofertaRepository = ofertaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.usuarioService = usuarioService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<OfertaDTO> buscar(Integer disciplinaId, Modalidade modalidade, BigDecimal precoMin,
                                  BigDecimal precoMax, String termo) {
        String t = (termo == null || termo.isBlank()) ? null : termo.trim();
        return paraDTO(ofertaRepository.buscar(disciplinaId, modalidade, precoMin, precoMax, t));
    }

    @Transactional(readOnly = true)
    public List<OfertaDTO> listarDoProfessor(Integer professorId) {
        return paraDTO(ofertaRepository.findByProfessorId(professorId));
    }

    @Transactional(readOnly = true)
    public List<OfertaDTO> listarTodas() {
        return paraDTO(ofertaRepository.findAllComDetalhes());
    }

    @Transactional
    public OfertaDTO criar(Integer professorId, OfertaRequest req) {
        OfertaAula o = new OfertaAula();
        o.setProfessor(usuarioService.obterProfessor(professorId));
        preencher(o, req);
        return paraDTO(List.of(ofertaRepository.save(o))).get(0);
    }

    @Transactional
    public OfertaDTO atualizar(Integer professorId, Integer ofertaId, OfertaRequest req) {
        OfertaAula o = obterDoProfessor(professorId, ofertaId);
        preencher(o, req);
        return paraDTO(List.of(o)).get(0);
    }

    @Transactional
    public void excluir(Integer professorId, Integer ofertaId) {
        OfertaAula o = obterDoProfessor(professorId, ofertaId);
        try {
            ofertaRepository.delete(o);
            ofertaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Esta oferta já possui agendamentos e não pode ser excluída.");
        }
    }

    /** Administrador ajusta a comissão da plataforma de uma oferta. */
    @Transactional
    public OfertaDTO alterarComissao(Integer ofertaId, BigDecimal percentual) {
        OfertaAula o = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new NaoEncontradoException("Oferta não encontrada."));
        o.setPercentualComissao(percentual);
        return paraDTO(List.of(o)).get(0);
    }

    private void preencher(OfertaAula o, OfertaRequest req) {
        o.setDisciplina(disciplinaRepository.findById(req.disciplinaId())
                .orElseThrow(() -> new NaoEncontradoException("Disciplina não encontrada.")));
        o.setModalidade(req.modalidade());
        o.setValorHora(req.valorHora());
        o.setPercentualComissao(req.percentualComissao() != null
                ? req.percentualComissao()
                : props.financeiro().comissaoPadrao());
    }

    private OfertaAula obterDoProfessor(Integer professorId, Integer ofertaId) {
        OfertaAula o = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new NaoEncontradoException("Oferta não encontrada."));
        if (!o.getProfessor().getId().equals(professorId)) {
            throw new AcessoNegadoException("a oferta pertence a outro professor.");
        }
        return o;
    }

    private List<OfertaDTO> paraDTO(List<OfertaAula> ofertas) {
        Map<Integer, Double> medias = new HashMap<>();
        Map<Integer, Long> totais = new HashMap<>();
        return ofertas.stream().map(o -> {
            Integer pid = o.getProfessor().getId();
            Double media = medias.computeIfAbsent(pid, avaliacaoRepository::mediaDoProfessor);
            Long total = totais.computeIfAbsent(pid, avaliacaoRepository::countByProfessorId);
            return OfertaDTO.de(o, media, total);
        }).toList();
    }
}
