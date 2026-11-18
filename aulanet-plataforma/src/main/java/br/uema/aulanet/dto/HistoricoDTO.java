package br.uema.aulanet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** RF17: histórico por período com totalizadores. */
public record HistoricoDTO(LocalDate inicio, LocalDate fim, long totalAulas, long concluidas, long confirmadas,
                           long solicitadas, long canceladas, BigDecimal horasMinistradas, BigDecimal receitaConcluidas,
                           List<AgendamentoDTO> aulas) {
}
