package br.uema.aulanet.service;

import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.Disciplina;
import br.uema.aulanet.repository.DisciplinaRepository;
import br.uema.aulanet.repository.OfertaAulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;
    private final OfertaAulaRepository ofertaRepository;

    public DisciplinaService(DisciplinaRepository disciplinaRepository, OfertaAulaRepository ofertaRepository) {
        this.disciplinaRepository = disciplinaRepository;
        this.ofertaRepository = ofertaRepository;
    }

    @Transactional(readOnly = true)
    public List<Disciplina> listar() {
        return disciplinaRepository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Disciplina criar(String nome) {
        String n = nome.trim();
        if (disciplinaRepository.existsByNomeIgnoreCase(n)) {
            throw new RegraNegocioException("Disciplina já cadastrada.");
        }
        return disciplinaRepository.save(new Disciplina(n));
    }

    /** Uma disciplina com ofertas vinculadas não pode ser excluída. */
    @Transactional
    public void excluir(Integer id) {
        Disciplina d = disciplinaRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Disciplina não encontrada."));
        if (ofertaRepository.existsByDisciplinaId(id)) {
            throw new RegraNegocioException("Disciplina com ofertas vinculadas não pode ser excluída.");
        }
        disciplinaRepository.delete(d);
    }
}
