package br.uema.aulanet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Perfil (dados, currículo, foto) e busca de professores, sobre os dados de demonstração. */
@SpringBootTest
@AutoConfigureMockMvc
class PerfilEBuscaIntegrationTest {

    /** Menor PNG válido (1×1 px). */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D,
            'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89};

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"123456\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private JsonNode getJson(String url) throws Exception {
        return json.readTree(mvc.perform(get(url))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private List<String> nomes(JsonNode lista) {
        List<String> n = new ArrayList<>();
        lista.forEach(p -> n.add(p.get("nome").asText()));
        return n;
    }

    @Test
    void professorEditaCurriculoEEleApareceNoPerfilPublico() throws Exception {
        String token = login("juliana@aulanet.com");
        mvc.perform(put("/api/usuarios/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Juliana Alves","telefone":"(86) 9 9999-0000","cidade":"Teresina","uf":"pi",
                                 "bio":"Professora de Português e Espanhol com foco em leitura, escrita e conversação.",
                                 "formacao":"Letras — UESPI","experiencia":"7 anos no ensino fundamental",
                                 "linkCurriculo":"https://lattes.cnpq.br/123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uf").value("PI"))
                .andExpect(jsonPath("$.faltando[0]").value("foto de perfil"));

        int id = json.readTree(mvc.perform(get("/api/usuarios/me").header("Authorization", token)).andReturn().getResponse().getContentAsString()).get("id").asInt();
        JsonNode publico = getJson("/api/professores/" + id);
        assertThat(publico.get("formacao").asText()).isEqualTo("Letras — UESPI");
        assertThat(publico.get("linkCurriculo").asText()).isEqualTo("https://lattes.cnpq.br/123");
    }

    @Test
    void linkDeCurriculoPrecisaSerHttp() throws Exception {
        mvc.perform(put("/api/usuarios/me").header("Authorization", login("rafael@aulanet.com")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rafael Costa\",\"linkCurriculo\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fotoDePerfilEnviadaFicaPublicaERemovivel() throws Exception {
        String token = login("carlos@aulanet.com");
        JsonNode perfil = json.readTree(mvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/foto")
                        .file(new MockMultipartFile("foto", "eu.png", "image/png", PNG)).header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String url = perfil.get("fotoUrl").asText();
        assertThat(url).startsWith("/api/usuarios/" + perfil.get("id").asInt() + "/foto?v=");

        // pública, com o tipo detectado pelos bytes
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
        assertThat(nomes(getJson("/api/professores?termo=carlos"))).containsExactly("Carlos Mendes");
        assertThat(getJson("/api/professores?termo=carlos").get(0).get("fotoUrl").asText()).isEqualTo(url);

        mvc.perform(delete("/api/usuarios/me/foto").header("Authorization", token))
                .andExpect(jsonPath("$.fotoUrl").doesNotExist());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void arquivoQueNaoEImagemERecusado() throws Exception {
        String token = login("ana@aulanet.com");
        // extensão e tipo dizem "imagem", mas o conteúdo é HTML: recusado pelos bytes
        mvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/foto")
                        .file(new MockMultipartFile("foto", "foto.png", "image/png", "<html><script>".getBytes()))
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(HttpMethod.PUT, "/api/usuarios/me/foto")
                        .file(new MockMultipartFile("foto", "foto.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buscaAgrupaPorProfessorEIgnoraAcentos() throws Exception {
        JsonNode todos = getJson("/api/professores");
        assertThat(nomes(todos)).doesNotHaveDuplicates().contains("Ana Souza", "Rafael Costa");

        // "matematica" sem acento encontra a disciplina "Matemática"; um cartão só, com a faixa de preço
        JsonNode mat = getJson("/api/professores?termo=matematica");
        assertThat(nomes(mat)).containsExactly("Ana Souza");
        assertThat(mat.get(0).get("precoMin").decimalValue()).isEqualByComparingTo("80");
        assertThat(mat.get(0).get("precoMax").decimalValue()).isEqualByComparingTo("100");

        assertThat(nomes(getJson("/api/professores?cidade=sao luis"))).containsExactly("Ana Souza");
        assertThat(nomes(getJson("/api/professores?cidade=timon"))).containsExactlyInAnyOrder("Beatriz Lima", "Rafael Costa");
        assertThat(nomes(getJson("/api/professores?modalidade=PRESENCIAL&precoMax=80"))).containsExactly("Rafael Costa");
        // nota mínima exclui quem ainda não tem avaliação
        assertThat(nomes(getJson("/api/professores?notaMin=4.5"))).doesNotContain("Rafael Costa", "Juliana Alves");
    }

    @Test
    void buscaOrdenaPorMenorPreco() throws Exception {
        JsonNode lista = getJson("/api/professores?ordenar=menor_preco");
        for (int i = 1; i < lista.size(); i++) {
            assertThat(lista.get(i).get("precoMin").decimalValue())
                    .isGreaterThanOrEqualTo(lista.get(i - 1).get("precoMin").decimalValue());
        }
    }
}
