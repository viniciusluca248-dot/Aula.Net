package br.uema.aulanet.service;

import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.Disciplina;
import br.uema.aulanet.repository.DisciplinaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;

    public DisciplinaService(DisciplinaRepository disciplinaRepository) {
        this.disciplinaRepository = disciplinaRepository;
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
}
