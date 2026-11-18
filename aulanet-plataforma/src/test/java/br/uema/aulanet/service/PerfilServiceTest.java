package br.uema.aulanet.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilServiceTest {

    @Test
    void reconheceImagensPelosPrimeirosBytes() {
        assertThat(PerfilService.tipoDaImagem(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("image/jpeg");
        assertThat(PerfilService.tipoDaImagem(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).isEqualTo("image/png");
        assertThat(PerfilService.tipoDaImagem("RIFF0000WEBPVP8 ".getBytes())).isEqualTo("image/webp");
    }

    @Test
    void recusaQualquerOutroConteudo() {
        assertThat(PerfilService.tipoDaImagem("<svg onload=alert(1)>".getBytes())).isNull();
        assertThat(PerfilService.tipoDaImagem("GIF89a".getBytes())).isNull();
        assertThat(PerfilService.tipoDaImagem(new byte[]{(byte) 0xFF})).isNull();
    }

    @Test
    void normalizacaoDaBuscaIgnoraAcentosEMaiusculas() {
        assertThat(ProfessorService.normalizar("  São Luís ")).isEqualTo("sao luis");
        assertThat(ProfessorService.normalizar("MATEMÁTICA")).isEqualTo("matematica");
        assertThat(ProfessorService.normalizar("  ")).isNull();
    }
}
