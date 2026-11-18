package br.uema.aulanet.dto;

import java.math.BigDecimal;
import java.util.List;

/** RF15: painel financeiro do professor. */
public record PainelFinanceiroDTO(int ano, int mes, BigDecimal receitaMes, BigDecimal receitaLiquidaMes,
                                  BigDecimal repassesRealizados, BigDecimal repassesPendentes,
                                  BigDecimal totalEmAberto, long aulasConcluidasMes,
                                  List<ReceitaMensal> ultimosMeses, List<ParcelaAberta> parcelasEmAberto,
                                  List<RepasseDTO> repasses) {

    public record ReceitaMensal(String mes, BigDecimal valor) {
    }

    public record ParcelaAberta(Integer parcelaId, String alunoNome, String disciplina, Integer numeroParcela,
                                Integer qtdParcelas, BigDecimal valorAPagar, java.time.LocalDate dataVencimento,
                                String status) {
    }
}
