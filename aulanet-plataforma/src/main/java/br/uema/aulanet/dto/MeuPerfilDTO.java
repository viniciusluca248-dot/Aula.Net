package br.uema.aulanet.dto;

import br.uema.aulanet.model.Papel;

import java.util.List;

/**
 * Perfil do usuário logado, com o indicador de completude: percentual preenchido e o que ainda falta,
 * para orientar o professor a montar um perfil que atraia alunos.
 */
public record MeuPerfilDTO(Integer id, String nome, String email, String cpf, String telefone, Papel papel,
                           String bio, String cidade, String uf, String formacao, String experiencia,
                           String linkCurriculo, String fotoUrl, int completude, List<String> faltando) {
}
