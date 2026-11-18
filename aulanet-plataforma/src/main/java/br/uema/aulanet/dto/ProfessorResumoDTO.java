package br.uema.aulanet.dto;

import java.math.BigDecimal;
import java.util.List;

/** Cartão de um professor na busca: um por professor, com as disciplinas e a faixa de preço das ofertas. */
public record ProfessorResumoDTO(Integer id, String nome, String fotoUrl, String cidade, String uf, String bioResumo,
                                 List<String> disciplinas, List<String> modalidades, BigDecimal precoMin,
                                 BigDecimal precoMax, Double mediaAvaliacao, long totalAvaliacoes,
                                 long aulasConcluidas) {
}
