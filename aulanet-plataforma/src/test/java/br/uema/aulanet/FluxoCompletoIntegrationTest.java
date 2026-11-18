package br.uema.aulanet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.uema.aulanet.repository.AgendamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa a API de ponta a ponta com os dados de demonstração (H2 em memória). */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoCompletoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AgendamentoRepository agendamentoRepository;

    private String login(String email, String senha) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode ler(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    @Test
    void endpointsProtegidosRetornam401SemToken() throws Exception {
        mvc.perform(get("/api/agendamentos/meus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/relatorio").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
    }

    @Test
    void buscaPublicaFunciona() throws Exception {
        mvc.perform(get("/api/ofertas").param("modalidade", "ONLINE").param("precoMax", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modalidade").value("ONLINE"));
    }

    @Test
    void loginInvalidoRetorna401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"joao@aulanet.com\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroPublicoNaoPodeCriarAdmin() throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"email\":\"x@x.com\",\"senha\":\"123456\",\"papel\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void controleDeAcessoPorPapel() throws Exception {
        String aluno = login("joao@aulanet.com", "123456");
        mvc.perform(auth(get("/api/admin/relatorio"), aluno)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/financeiro/painel"), aluno)).andExpect(status().isForbidden());
        String admin = login("admin@aulanet.com", "admin123");
        mvc.perform(auth(get("/api/admin/relatorio"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAulas").isNumber());
    }

    @Test
    void fluxoDoCadastroAoRepasse() throws Exception {
        // 1. cadastro de professor e aluno (o cadastro já devolve o token de login)
        String prof = json.readTree(mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Prof Fluxo\",\"email\":\"prof.fluxo@teste.com\",\"senha\":\"123456\",\"papel\":\"PROFESSOR\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Aluno Fluxo\",\"email\":\"aluno.fluxo@teste.com\",\"senha\":\"123456\",\"papel\":\"ALUNO\"}"))
                .andExpect(status().isCreated());
        String aluno = login("aluno.fluxo@teste.com", "123456");
        String admin = login("admin@aulanet.com", "admin123");

        // 2. professor cadastra a oferta: R$ 100/h com 15% de comissão
        int disciplinaId = ler(mvc.perform(get("/api/disciplinas"))).get(0).get("id").asInt();
        int ofertaId = ler(mvc.perform(auth(post("/api/ofertas"), prof).content(
                        "{\"disciplinaId\":" + disciplinaId + ",\"modalidade\":\"ONLINE\",\"valorHora\":100,\"percentualComissao\":15}"))
                .andExpect(status().isCreated())).get("id").asInt();

        // 3. aluno solicita 3 horas de aula
        String data = LocalDate.now().plusDays(20).toString();
        String pedido = "{\"ofertaId\":" + ofertaId + ",\"dataAula\":\"" + data + "\",\"horarioInicio\":\"07:00\",\"duracaoMinutos\":180}";
        int agId = ler(mvc.perform(auth(post("/api/agendamentos"), aluno).content(pedido))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SOLICITADO"))).get("id").asInt();
        // o aluno não pode ter outra aula no mesmo horário
        mvc.perform(auth(post("/api/agendamentos"), aluno).content(pedido)).andExpect(status().isBadRequest());
        // concluir sem confirmar é rejeitado
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(status().isBadRequest());

        // 4. professor confirma; a aula ainda não aconteceu, então não pode ser concluída
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/confirmar"), prof)).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(status().isBadRequest());
        // simula a passagem do tempo: a aula passa a ter sido ontem
        var aula = agendamentoRepository.findById(agId).orElseThrow();
        aula.setDataAula(LocalDate.now().minusDays(1));
        agendamentoRepository.save(aula);

        // 5. conclusão gera pagamento e repasse; não pode concluir duas vezes
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(jsonPath("$.status").value("CONCLUIDO"));
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(status().isBadRequest());

        // 6. aluno simula e parcela em 3x pela Tabela Price
        mvc.perform(auth(get("/api/pagamentos/simulacao").param("valor", "300").param("parcelas", "3"), aluno))
                .andExpect(jsonPath("$.taxaEfetivaAnual").value(0.2682))
                .andExpect(jsonPath("$.valorAVista").value(300.0));
        JsonNode pg = ler(mvc.perform(auth(get("/api/pagamentos/meus"), aluno))).get(0);
        assertThat(pg.get("valorTotal").decimalValue()).isEqualByComparingTo("300.00");
        JsonNode parcelado = ler(mvc.perform(auth(post("/api/pagamentos/" + pg.get("id").asInt() + "/parcelamento"), aluno)
                .content("{\"qtdParcelas\":3}")).andExpect(status().isOk()));
        assertThat(parcelado.get("tipo").asText()).isEqualTo("PARCELADO");
        assertThat(parcelado.get("parcelas")).hasSize(3);

        // 7. repasse: R$ 300 − 15% = R$ 255; só pode ser realizado depois da quitação
        JsonNode repasse = null;
        for (JsonNode r : ler(mvc.perform(auth(get("/api/admin/repasses"), admin)))) {
            if (r.get("pagamentoId").asInt() == pg.get("id").asInt()) repasse = r;
        }
        assertThat(repasse.get("valorLiquido").decimalValue()).isEqualByComparingTo("255.00");
        mvc.perform(auth(patch("/api/admin/repasses/" + repasse.get("id").asInt() + "/realizar"), admin))
                .andExpect(status().isBadRequest());
        JsonNode depois = null;
        for (JsonNode parcela : parcelado.get("parcelas")) {
            depois = ler(mvc.perform(auth(post("/api/parcelas/" + parcela.get("id").asInt() + "/pagar"), aluno)).andExpect(status().isOk()));
        }
        assertThat(depois.get("quitado").asBoolean()).isTrue();
        mvc.perform(auth(patch("/api/admin/repasses/" + repasse.get("id").asInt() + "/realizar"), admin))
                .andExpect(jsonPath("$.status").value("REALIZADO"));

        // 8. aluno avalia uma única vez; professor vê o repasse no painel
        String avaliacao = "{\"agendamentoId\":" + agId + ",\"nota\":5,\"comentario\":\"Top\"}";
        mvc.perform(auth(post("/api/avaliacoes"), aluno).content(avaliacao)).andExpect(status().isCreated());
        mvc.perform(auth(post("/api/avaliacoes"), aluno).content(avaliacao)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/financeiro/painel"), prof)).andExpect(jsonPath("$.repassesRealizados").value(255.0));
    }
}
