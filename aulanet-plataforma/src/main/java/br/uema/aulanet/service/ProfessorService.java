package br.uema.aulanet.service;

import br.uema.aulanet.dto.AvaliacaoDTO;
import br.uema.aulanet.dto.OfertaDTO;
import br.uema.aulanet.dto.ProfessorPerfilDTO;
import br.uema.aulanet.model.Usuario;
import br.uema.aulanet.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** RF05: perfil público do professor. */
@Service
public class ProfessorService {

    private final UsuarioService usuarioService;
    private final OfertaService ofertaService;
    private final AvaliacaoRepository avaliacaoRepository;

    public ProfessorService(UsuarioService usuarioService, OfertaService ofertaService,
                            AvaliacaoRepository avaliacaoRepository) {
        this.usuarioService = usuarioService;
        this.ofertaService = ofertaService;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Transactional(readOnly = true)
    public ProfessorPerfilDTO perfil(Integer professorId) {
        Usuario p = usuarioService.obterProfessor(professorId);
        List<OfertaDTO> ofertas = ofertaService.listarDoProfessor(professorId);
        List<AvaliacaoDTO> avaliacoes = avaliacaoRepository.findByProfessor(professorId).stream()
                .map(AvaliacaoDTO::de).toList();
        return new ProfessorPerfilDTO(p.getId(), p.getNome(), p.getBio(),
                ofertas.stream().map(OfertaDTO::disciplina).collect(Collectors.toCollection(TreeSet::new)),
                ofertas.stream().map(o -> o.modalidade().name()).collect(Collectors.toCollection(TreeSet::new)),
                avaliacaoRepository.mediaDoProfessor(professorId), avaliacoes.size(), ofertas, avaliacoes);
    }
}
