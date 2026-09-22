package br.uema.aulanet.dto;

import java.math.BigDecimal;
import java.util.Map;

/** RF18: relatórios globais da plataforma. */
public record RelatorioAdminDTO(long totalUsuarios, long totalAlunos, long totalProfessores, long totalAulas,
                                Map<String, Long> aulasPorStatus, BigDecimal receitaPlataforma,
                                long repassesPendentesQtd, BigDecimal repassesPendentesValor,
                                BigDecimal repassesRealizadosValor, long parcelasAtrasadas) {
}
