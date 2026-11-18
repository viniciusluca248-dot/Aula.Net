package br.uema.aulanet.dto;

import br.uema.aulanet.model.Repasse;
import br.uema.aulanet.model.StatusRepasse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepasseDTO(Integer id, Integer pagamentoId, Integer professorId, String professorNome, String alunoNome,
                         String disciplina, LocalDate dataAula, BigDecimal valorBruto, BigDecimal valorComissao,
                         BigDecimal valorLiquido, StatusRepasse status, boolean pagamentoQuitado) {

    public static RepasseDTO de(Repasse r) {
        var a = r.getPagamento().getAgendamento();
        return new RepasseDTO(r.getId(), r.getPagamento().getId(), r.getProfessor().getId(), r.getProfessor().getNome(),
                a.getAluno().getNome(), a.getOferta().getDisciplina().getNome(), a.getDataAula(), r.getValorBruto(),
                r.getValorComissao(), r.getValorLiquido(), r.getStatus(), r.getPagamento().isQuitado());
    }
}
