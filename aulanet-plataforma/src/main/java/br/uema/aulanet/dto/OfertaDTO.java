package br.uema.aulanet.dto;

import br.uema.aulanet.model.Modalidade;
import br.uema.aulanet.model.OfertaAula;

import java.math.BigDecimal;

public record OfertaDTO(Integer id, Integer professorId, String professorNome, Integer disciplinaId, String disciplina,
                        Modalidade modalidade, BigDecimal valorHora, BigDecimal percentualComissao,
                        Double mediaAvaliacao, Long totalAvaliacoes) {

    public static OfertaDTO de(OfertaAula o, Double media, Long total) {
        return new OfertaDTO(o.getId(), o.getProfessor().getId(), o.getProfessor().getNome(),
                o.getDisciplina().getId(), o.getDisciplina().getNome(), o.getModalidade(),
                o.getValorHora(), o.getPercentualComissao(), media, total);
    }
}
