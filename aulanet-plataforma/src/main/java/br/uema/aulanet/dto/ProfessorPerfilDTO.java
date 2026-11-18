package br.uema.aulanet.dto;

import java.util.List;
import java.util.Set;

/** RF06: perfil público do professor, com bio, currículo e avaliações. */
public record ProfessorPerfilDTO(Integer id, String nome, String fotoUrl, String bio, String cidade, String uf,
                                 String formacao, String experiencia, String linkCurriculo,
                                 Set<String> disciplinas, Set<String> modalidades, Double mediaAvaliacao,
                                 long totalAvaliacoes, long aulasConcluidas, List<OfertaDTO> ofertas,
                                 List<AvaliacaoDTO> avaliacoes) {
}
