package br.uema.aulanet.dto;

import br.uema.aulanet.model.Pagamento;
import br.uema.aulanet.model.TipoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PagamentoDTO(Integer id, Integer agendamentoId, String disciplina, String professorNome, String alunoNome,
                           LocalDate dataAula, BigDecimal valorTotal, TipoPagamento tipo, Integer qtdParcelas,
                           boolean quitado, boolean podeParcelar, List<ParcelaDTO> parcelas) {

    public static PagamentoDTO de(Pagamento p, boolean podeParcelar) {
        var a = p.getAgendamento();
        return new PagamentoDTO(p.getId(), a.getId(), a.getOferta().getDisciplina().getNome(),
                a.getOferta().getProfessor().getNome(), a.getAluno().getNome(), a.getDataAula(), p.getValorTotal(),
                p.getTipo(), p.getQtdParcelas(), p.isQuitado(), podeParcelar,
                p.getParcelas().stream().map(ParcelaDTO::de).toList());
    }
}
