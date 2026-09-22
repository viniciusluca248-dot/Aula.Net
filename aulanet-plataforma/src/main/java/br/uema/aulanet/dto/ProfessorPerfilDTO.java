package br.uema.aulanet.dto;

import java.util.List;
import java.util.Set;

/** RF05: perfil público do professor. */
public record ProfessorPerfilDTO(Integer id, String nome, String bio, Set<String> disciplinas, Set<String> modalidades,
                                 Double mediaAvaliacao, long totalAvaliacoes, List<OfertaDTO> ofertas,
                                 List<AvaliacaoDTO> avaliacoes) {
}
