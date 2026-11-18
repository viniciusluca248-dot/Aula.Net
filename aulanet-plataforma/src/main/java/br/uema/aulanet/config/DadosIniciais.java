package br.uema.aulanet.config;

import br.uema.aulanet.model.*;
import br.uema.aulanet.repository.*;
import br.uema.aulanet.service.FinanceiroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

/**
 * Popula o banco com dados fictícios (seeds) na primeira execução, para demonstração e testes de usabilidade.
 * Desative com aulanet.seed=false.
 */
@Component
@ConditionalOnProperty(name = "aulanet.seed", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);
    private static final String SENHA_PADRAO = "123456";

    private final UsuarioRepository usuarios;
    private final DisciplinaRepository disciplinas;
    private final OfertaAulaRepository ofertas;
    private final AgendamentoRepository agendamentos;
    private final AvaliacaoRepository avaliacoes;
    private final PagamentoRepository pagamentos;
    private final RepasseRepository repasses;
    private final FinanceiroService financeiro;
    private final PasswordEncoder encoder;
    private final PerfilUsuarioRepository perfis;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DadosIniciais(UsuarioRepository usuarios, DisciplinaRepository disciplinas, OfertaAulaRepository ofertas,
                         AgendamentoRepository agendamentos, AvaliacaoRepository avaliacoes,
                         PagamentoRepository pagamentos, RepasseRepository repasses, FinanceiroService financeiro,
                         PasswordEncoder encoder, TransactionTemplate tx, Clock clock,
                         PerfilUsuarioRepository perfis) {
        this.perfis = perfis;
        this.usuarios = usuarios;
        this.disciplinas = disciplinas;
        this.ofertas = ofertas;
        this.agendamentos = agendamentos;
        this.avaliacoes = avaliacoes;
        this.pagamentos = pagamentos;
        this.repasses = repasses;
        this.financeiro = financeiro;
        this.encoder = encoder;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return;
        }
        tx.executeWithoutResult(s -> popular());
        log.info("""

                ===================== Aula.net: dados de demonstração criados =====================
                 ADMIN     admin@aulanet.com   / admin123
                 PROFESSOR ana@aulanet.com     / 123456   (Matemática, Física)
                 PROFESSOR carlos@aulanet.com  / 123456   (Inglês)
                 PROFESSOR beatriz@aulanet.com / 123456   (Programação, Redação)
                 PROFESSOR rafael@aulanet.com  / 123456   (Química, Biologia)
                 PROFESSOR juliana@aulanet.com / 123456   (Português, Espanhol)
                 ALUNO     joao@aulanet.com    / 123456
                 ALUNO     maria@aulanet.com   / 123456
                ====================================================================================
                """);
    }

    private void popular() {
        Map<String, Disciplina> d = new HashMap<>();
        for (String nome : new String[]{"Matemática", "Física", "Química", "Biologia", "Português", "Redação",
                "Inglês", "Espanhol", "História", "Geografia", "Programação"}) {
            d.put(nome, disciplinas.save(new Disciplina(nome)));
        }

        usuario("Administrador Aula.net", "admin@aulanet.com", "admin123", null, Papel.ADMIN, null);
        Usuario ana = usuario("Ana Souza", "ana@aulanet.com", SENHA_PADRAO, "11122233344", Papel.PROFESSOR,
                "Licenciada em Matemática pela UEMA, 8 anos de experiência com ensino médio, ENEM e vestibulares.");
        Usuario carlos = usuario("Carlos Mendes", "carlos@aulanet.com", SENHA_PADRAO, "22233344455", Papel.PROFESSOR,
                "Professor de inglês com certificação CAE. Aulas de conversação, business English e preparação para exames.");
        Usuario beatriz = usuario("Beatriz Lima", "beatriz@aulanet.com", SENHA_PADRAO, "33344455566", Papel.PROFESSOR,
                "Desenvolvedora e professora de programação (Java, Python, JavaScript). Também corrijo redações para o ENEM.");
        Usuario joao = usuario("João Pereira", "joao@aulanet.com", SENHA_PADRAO, "44455566677", Papel.ALUNO, null);
        Usuario maria = usuario("Maria Oliveira", "maria@aulanet.com", SENHA_PADRAO, "55566677788", Papel.ALUNO,
                "Mãe de dois filhos em homeschooling.");

        Usuario rafael = usuario("Rafael Costa", "rafael@aulanet.com", SENHA_PADRAO, "66677788899", Papel.PROFESSOR,
                "Biólogo e professor de Química e Biologia para o ENEM. Aulas com mapas mentais e muitos exercícios resolvidos.");
        Usuario juliana = usuario("Juliana Alves", "juliana@aulanet.com", SENHA_PADRAO, "77788899900", Papel.PROFESSOR,
                "Professora de Português e Espanhol. Atendo crianças em educação domiciliar e adultos que precisam de reforço.");

        perfil(ana, "São Luís", "MA", "Licenciatura em Matemática — UEMA (2016)\nEspecialização em Ensino de Matemática — UFMA (2019)",
                "8 anos em escolas particulares de São Luís\nMonitora de pré-vestibular desde 2018\nMais de 300 alunos aprovados no ENEM",
                "https://lattes.cnpq.br/0000000000000001");
        perfil(carlos, "Teresina", "PI", "Letras Inglês — UFPI (2014)\nCertificado Cambridge C1 Advanced (CAE)",
                "Professor de cursos livres de inglês por 10 anos\nAulas de business English para empresas", null);
        perfil(beatriz, "Timon", "MA", "Bacharelado em Ciência da Computação — UFPI (2018)",
                "Desenvolvedora back-end Java desde 2018\nCorretora de redações do ENEM desde 2021",
                "https://www.linkedin.com/in/exemplo-beatriz");
        perfil(rafael, "Timon", "MA", "Licenciatura em Ciências Biológicas — UEMA (2017)",
                "Professor de cursinho pré-vestibular há 6 anos", null);
        perfil(juliana, "Teresina", "PI", "Licenciatura em Letras Português/Espanhol — UESPI (2015)",
                "Professora do ensino fundamental por 7 anos\nAulas para famílias em homeschooling desde 2020", null);

        OfertaAula anaMatOnline = oferta(ana, d.get("Matemática"), Modalidade.ONLINE, "80.00", "10.00");
        OfertaAula anaMatPres = oferta(ana, d.get("Matemática"), Modalidade.PRESENCIAL, "100.00", "10.00");
        OfertaAula anaFis = oferta(ana, d.get("Física"), Modalidade.ONLINE, "90.00", "10.00");
        OfertaAula carlosIng = oferta(carlos, d.get("Inglês"), Modalidade.ONLINE, "70.00", "12.00");
        OfertaAula carlosIngPres = oferta(carlos, d.get("Inglês"), Modalidade.PRESENCIAL, "95.00", "12.00");
        OfertaAula biaProg = oferta(beatriz, d.get("Programação"), Modalidade.ONLINE, "120.00", "15.00");
        OfertaAula biaRed = oferta(beatriz, d.get("Redação"), Modalidade.ONLINE, "60.00", "10.00");
        oferta(rafael, d.get("Química"), Modalidade.PRESENCIAL, "75.00", "10.00");
        oferta(rafael, d.get("Biologia"), Modalidade.ONLINE, "65.00", "10.00");
        oferta(juliana, d.get("Português"), Modalidade.ONLINE, "55.00", "10.00");
        oferta(juliana, d.get("Espanhol"), Modalidade.ONLINE, "60.00", "10.00");

        LocalDate hoje = LocalDate.now(clock);
        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Histórico concluído nos últimos meses (alimenta gráficos e relatórios)
        concluida(joao, anaMatOnline, hoje.minusMonths(4).withDayOfMonth(5), "14:00", 60, true, 5, "Explica muito bem!");
        concluida(maria, anaMatPres, hoje.minusMonths(3).withDayOfMonth(10), "09:00", 120, true, 5, "Ótima com crianças.");
        concluida(joao, anaFis, hoje.minusMonths(2).withDayOfMonth(12), "16:00", 90, true, 4, "Boa didática.");
        concluida(maria, carlosIng, hoje.minusMonths(2).withDayOfMonth(20), "18:00", 60, true, 5, "Aula dinâmica.");
        concluida(joao, biaProg, hoje.minusMonths(1).withDayOfMonth(8), "19:00", 120, true, 5, "Aprendi Spring Boot!");
        // aluno já quitou, mas o administrador ainda não registrou a transferência: repasse liberado
        Agendamento quitada = concluida(joao, biaRed, hoje.minusDays(12), "17:00", 60, true, 0, null);
        pagamentos.findByAluno(joao.getId()).stream()
                .filter(p -> p.getAgendamento().getId().equals(quitada.getId())).findFirst()
                .flatMap(p -> repasses.findByPagamentoId(p.getId()))
                .ifPresent(r -> r.setStatus(StatusRepasse.PENDENTE));
        concluida(maria, biaRed, hoje.minusMonths(1).withDayOfMonth(15), "10:00", 60, false, 0, null);

        // Aula concluída, parcelada em 3x, com a 1ª parcela vencida (demonstra multa por atraso - RF13)
        Agendamento parcelada = concluida(joao, anaMatPres, hoje.minusDays(40), "15:00", 180, false, 4, "Muito paciente.");
        Pagamento pg = pagamentos.findByAluno(joao.getId()).stream()
                .filter(p -> p.getAgendamento().getId().equals(parcelada.getId())).findFirst().orElseThrow();
        financeiro.parcelar(joao.getId(), pg.getId(), 3);
        pagamentos.findDetalhado(pg.getId()).ifPresent(p ->
                p.getParcelas().forEach(pa -> pa.setDataVencimento(hoje.minusDays(10).plusMonths(pa.getNumeroParcela() - 1L))));

        // Aula concluída recentemente, pagamento à vista pendente
        concluida(maria, carlosIngPres, hoje.minusDays(3), "11:00", 60, false, 0, null);
        concluida(maria, anaMatOnline, hoje.withDayOfMonth(1), "11:00", 90, false, 0, null);

        // Agenda da semana atual (RF10) e solicitações pendentes (RF08)
        agendamento(joao, anaMatOnline, segunda.plusWeeks(1), "14:00", 60, StatusAgendamento.CONFIRMADO);
        agendamento(maria, anaFis, segunda.plusWeeks(1).plusDays(2), "10:00", 90, StatusAgendamento.CONFIRMADO);
        agendamento(joao, anaMatOnline, hoje.plusDays(1), "08:00", 60, StatusAgendamento.CONFIRMADO);
        agendamento(maria, anaMatPres, hoje.plusDays(2), "16:00", 120, StatusAgendamento.CONFIRMADO);
        agendamento(joao, anaFis, hoje.plusDays(3), "19:00", 60, StatusAgendamento.SOLICITADO);
        agendamento(maria, anaMatOnline, hoje.plusDays(5), "09:00", 60, StatusAgendamento.SOLICITADO);
        agendamento(joao, carlosIng, hoje.plusDays(2), "18:00", 60, StatusAgendamento.SOLICITADO);
        agendamento(maria, biaProg, hoje.plusDays(4), "20:00", 90, StatusAgendamento.CONFIRMADO);
        agendamento(joao, biaRed, hoje.minusDays(20), "10:00", 60, StatusAgendamento.CANCELADO);
    }

    private Usuario usuario(String nome, String email, String senha, String cpf, Papel papel, String bio) {
        Usuario u = new Usuario(nome, email, encoder.encode(senha), cpf, "(98) 9 9999-0000", papel);
        u.setBio(bio);
        return usuarios.save(u);
    }

    private void perfil(Usuario u, String cidade, String uf, String formacao, String experiencia, String link) {
        PerfilUsuario p = new PerfilUsuario(u);
        p.setCidade(cidade);
        p.setUf(uf);
        p.setFormacao(formacao);
        p.setExperiencia(experiencia);
        p.setLinkCurriculo(link);
        perfis.save(p);
    }

    private OfertaAula oferta(Usuario prof, Disciplina disc, Modalidade mod, String valor, String comissao) {
        OfertaAula o = new OfertaAula();
        o.setProfessor(prof);
        o.setDisciplina(disc);
        o.setModalidade(mod);
        o.setValorHora(new BigDecimal(valor));
        o.setPercentualComissao(new BigDecimal(comissao));
        return ofertas.save(o);
    }

    private Agendamento agendamento(Usuario aluno, OfertaAula oferta, LocalDate data, String hora, int minutos,
                                    StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setAluno(aluno);
        a.setOferta(oferta);
        a.setDataAula(data);
        a.setHorarioInicio(LocalTime.parse(hora));
        a.setDuracaoMinutos(minutos);
        a.setStatus(status);
        return agendamentos.save(a);
    }

    private Agendamento concluida(Usuario aluno, OfertaAula oferta, LocalDate data, String hora, int minutos,
                                  boolean pago, int nota, String comentario) {
        Agendamento a = agendamento(aluno, oferta, data, hora, minutos, StatusAgendamento.CONCLUIDO);
        Pagamento p = financeiro.gerarPagamento(a);
        p.getParcelas().forEach(pa -> pa.setDataVencimento(data.plusDays(7)));
        if (pago) {
            p.getParcelas().forEach(pa -> pa.setStatus(StatusParcela.PAGO));
            repasses.findByPagamentoId(p.getId()).ifPresent(r -> r.setStatus(StatusRepasse.REALIZADO));
        }
        if (nota > 0) {
            Avaliacao av = new Avaliacao();
            av.setAgendamento(a);
            av.setAluno(aluno);
            av.setProfessor(oferta.getProfessor());
            av.setNota(nota);
            av.setComentario(comentario);
            avaliacoes.save(av);
        }
        return a;
    }
}
