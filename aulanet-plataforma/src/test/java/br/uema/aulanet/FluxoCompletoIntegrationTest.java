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
    void fluxoSolicitarConfirmarConcluirParcelarPagar() throws Exception {
        String aluno = login("maria@aulanet.com", "123456");
        String prof = login("carlos@aulanet.com", "123456");

        JsonNode ofertas = ler(mvc.perform(get("/api/ofertas").param("termo", "Carlos")));
        int ofertaId = ofertas.get(0).get("id").asInt();
        String data = LocalDate.now().plusDays(20).toString();

        // 1. aluno solicita
        JsonNode ag = ler(mvc.perform(auth(post("/api/agendamentos"), aluno).content(
                        "{\"ofertaId\":" + ofertaId + ",\"dataAula\":\"" + data + "\",\"horarioInicio\":\"07:00\",\"duracaoMinutos\":120}"))
                .andExpect(status().isCreated()));
        int agId = ag.get("id").asInt();
        assertThat(ag.get("status").asText()).isEqualTo("SOLICITADO");

        // 2. professor confirma e 3. conclui (gera pagamento)
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/confirmar"), prof)).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        // aula ainda não aconteceu: não pode ser concluída
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(status().isBadRequest());
        // simula a passagem do tempo: a aula passa a ter sido ontem
        var aula = agendamentoRepository.findById(agId).orElseThrow();
        aula.setDataAula(LocalDate.now().minusDays(1));
        agendamentoRepository.save(aula);
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(jsonPath("$.status").value("CONCLUIDO"));
        mvc.perform(auth(patch("/api/agendamentos/" + agId + "/concluir"), prof)).andExpect(status().isBadRequest());

        // 4. aluno parcela em 2x e paga
        JsonNode pagamentos = ler(mvc.perform(auth(get("/api/pagamentos/meus"), aluno)));
        JsonNode pg = null;
        for (JsonNode p : pagamentos) if (p.get("agendamentoId").asInt() == agId) pg = p;
        assertThat(pg).isNotNull();
        JsonNode parcelado = ler(mvc.perform(auth(post("/api/pagamentos/" + pg.get("id").asInt() + "/parcelamento"), aluno)
                .content("{\"qtdParcelas\":2}")).andExpect(status().isOk()));
        assertThat(parcelado.get("tipo").asText()).isEqualTo("PARCELADO");
        assertThat(parcelado.get("parcelas")).hasSize(2);

        JsonNode depois = null;
        for (JsonNode parcela : parcelado.get("parcelas")) {
            depois = ler(mvc.perform(auth(post("/api/parcelas/" + parcela.get("id").asInt() + "/pagar"), aluno)).andExpect(status().isOk()));
        }
        assertThat(depois.get("quitado").asBoolean()).isTrue();

        // 5. aluno avalia; professor vê o painel
        mvc.perform(auth(post("/api/avaliacoes"), aluno).content("{\"agendamentoId\":" + agId + ",\"nota\":5,\"comentario\":\"Top\"}"))
                .andExpect(status().isCreated());
        mvc.perform(auth(get("/api/financeiro/painel"), prof)).andExpect(status().isOk())
                .andExpect(jsonPath("$.repassesRealizados").isNumber());
    }
}
