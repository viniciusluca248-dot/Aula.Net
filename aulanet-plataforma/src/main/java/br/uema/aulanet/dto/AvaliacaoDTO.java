package br.uema.aulanet.dto;

import br.uema.aulanet.model.Avaliacao;

public record AvaliacaoDTO(Integer id, Integer agendamentoId, String alunoNome, Integer nota, String comentario) {

    public static AvaliacaoDTO de(Avaliacao a) {
        return new AvaliacaoDTO(a.getId(), a.getAgendamento().getId(), a.getAluno().getNome(), a.getNota(), a.getComentario());
    }
}
