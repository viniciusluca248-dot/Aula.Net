package br.uema.aulanet.service;

import br.uema.aulanet.dto.AvaliacaoDTO;
import br.uema.aulanet.dto.OfertaDTO;
import br.uema.aulanet.dto.ProfessorPerfilDTO;
import br.uema.aulanet.dto.ProfessorResumoDTO;
import br.uema.aulanet.model.Modalidade;
import br.uema.aulanet.model.OfertaAula;
import br.uema.aulanet.model.PerfilUsuario;
import br.uema.aulanet.model.StatusAgendamento;
import br.uema.aulanet.model.Usuario;
import br.uema.aulanet.repository.AgendamentoRepository;
import br.uema.aulanet.repository.AvaliacaoRepository;
import br.uema.aulanet.repository.OfertaAulaRepository;
import br.uema.aulanet.repository.PerfilUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** RF05 / RF06: busca de professores e perfil público. */
@Service
public class ProfessorService {

    private final UsuarioService usuarioService;
    private final OfertaService ofertaService;
    private final OfertaAulaRepository ofertaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PerfilUsuarioRepository perfilRepository;

    public ProfessorService(UsuarioService usuarioService, OfertaService ofertaService,
                            OfertaAulaRepository ofertaRepository, AvaliacaoRepository avaliacaoRepository,
                            AgendamentoRepository agendamentoRepository, PerfilUsuarioRepository perfilRepository) {
        this.usuarioService = usuarioService;
        this.ofertaService = ofertaService;
        this.ofertaRepository = ofertaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.perfilRepository = perfilRepository;
    }

    /**
     * Busca com um cartão por professor. O termo procura no nome do professor e nas disciplinas; os demais
     * filtros são opcionais e combináveis. Ordenação: relevancia (padrão), menor_preco, maior_nota, mais_aulas.
     */
    @Transactional(readOnly = true)
    public List<ProfessorResumoDTO> buscar(String termo, Integer disciplinaId, Modalidade modalidade,
                                           BigDecimal precoMin, BigDecimal precoMax, String cidade,
                                           Double notaMin, String ordenar) {
        Map<Usuario, List<OfertaAula>> porProfessor = ofertaRepository
                .buscar(disciplinaId, modalidade, precoMin, precoMax, null).stream()
                .collect(Collectors.groupingBy(OfertaAula::getProfessor, LinkedHashMap::new, Collectors.toList()));

        Map<Integer, PerfilUsuario> perfis = perfilRepository
                .findAllById(porProfessor.keySet().stream().map(Usuario::getId).toList()).stream()
                .collect(Collectors.toMap(PerfilUsuario::getId, Function.identity()));
        Map<Integer, Object[]> notas = avaliacaoRepository.estatisticasPorProfessor().stream()
                .collect(Collectors.toMap(r -> (Integer) r[0], Function.identity()));
        Map<Integer, Long> aulas = agendamentoRepository.contarPorProfessor(StatusAgendamento.CONCLUIDO).stream()
                .collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));

        String t = normalizar(termo);
        String c = normalizar(cidade);
        List<ProfessorResumoDTO> resultado = new ArrayList<>();
        porProfessor.forEach((prof, ofertas) -> {
            PerfilUsuario perfil = perfis.get(prof.getId());
            List<String> disciplinas = ofertas.stream().map(o -> o.getDisciplina().getNome()).distinct().sorted().toList();
            if (t != null && !normalizar(prof.getNome()).contains(t)
                    && disciplinas.stream().noneMatch(d -> normalizar(d).contains(t))) {
                return;
            }
            if (c != null && (perfil == null || perfil.getCidade() == null || !normalizar(perfil.getCidade()).contains(c))) {
                return;
            }
            Object[] nota = notas.get(prof.getId());
            Double media = nota == null ? null : (Double) nota[1];
            if (notaMin != null && (media == null || media < notaMin)) {
                return;
            }
            resultado.add(new ProfessorResumoDTO(prof.getId(), prof.getNome(), PerfilService.fotoUrl(perfil),
                    perfil == null ? null : perfil.getCidade(), perfil == null ? null : perfil.getUf(),
                    resumo(prof.getBio()), disciplinas,
                    ofertas.stream().map(o -> o.getModalidade().name()).distinct().sorted().toList(),
                    ofertas.stream().map(OfertaAula::getValorHora).min(Comparator.naturalOrder()).orElse(null),
                    ofertas.stream().map(OfertaAula::getValorHora).max(Comparator.naturalOrder()).orElse(null),
                    media, nota == null ? 0 : (Long) nota[2], aulas.getOrDefault(prof.getId(), 0L)));
        });
        resultado.sort(ordem(ordenar));
        return resultado;
    }

    @Transactional(readOnly = true)
    public ProfessorPerfilDTO perfil(Integer professorId) {
        Usuario p = usuarioService.obterProfessor(professorId);
        PerfilUsuario perfil = perfilRepository.findById(professorId).orElse(null);
        List<OfertaDTO> ofertas = ofertaService.listarDoProfessor(professorId);
        List<AvaliacaoDTO> avaliacoes = avaliacaoRepository.findByProfessor(professorId).stream()
                .map(AvaliacaoDTO::de).toList();
        long aulas = agendamentoRepository.contarPorProfessor(StatusAgendamento.CONCLUIDO).stream()
                .filter(r -> professorId.equals(r[0])).mapToLong(r -> (Long) r[1]).sum();
        return new ProfessorPerfilDTO(p.getId(), p.getNome(), PerfilService.fotoUrl(perfil), p.getBio(),
                perfil == null ? null : perfil.getCidade(), perfil == null ? null : perfil.getUf(),
                perfil == null ? null : perfil.getFormacao(), perfil == null ? null : perfil.getExperiencia(),
                perfil == null ? null : perfil.getLinkCurriculo(),
                ofertas.stream().map(OfertaDTO::disciplina).collect(Collectors.toCollection(TreeSet::new)),
                ofertas.stream().map(o -> o.modalidade().name()).collect(Collectors.toCollection(TreeSet::new)),
                avaliacaoRepository.mediaDoProfessor(professorId), avaliacoes.size(), aulas, ofertas, avaliacoes);
    }

    private static Comparator<ProfessorResumoDTO> ordem(String ordenar) {
        Comparator<ProfessorResumoDTO> porNota = Comparator.comparing(
                (ProfessorResumoDTO p) -> p.mediaAvaliacao() == null ? 0d : p.mediaAvaliacao()).reversed();
        Comparator<ProfessorResumoDTO> porAvaliacoes = Comparator.comparingLong(ProfessorResumoDTO::totalAvaliacoes).reversed();
        Comparator<ProfessorResumoDTO> porPreco = Comparator.comparing(ProfessorResumoDTO::precoMin,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<ProfessorResumoDTO> porNome = Comparator.comparing(ProfessorResumoDTO::nome);
        return switch (ordenar == null ? "" : ordenar) {
            case "menor_preco" -> porPreco.thenComparing(porNota).thenComparing(porNome);
            case "maior_nota" -> porNota.thenComparing(porAvaliacoes).thenComparing(porNome);
            case "mais_aulas" -> Comparator.comparingLong(ProfessorResumoDTO::aulasConcluidas).reversed()
                    .thenComparing(porNota).thenComparing(porNome);
            // relevância: quem tem boa nota e mais avaliações aparece primeiro; sem avaliação, por preço
            default -> porNota.thenComparing(porAvaliacoes).thenComparing(porPreco).thenComparing(porNome);
        };
    }

    private static String resumo(String bio) {
        if (bio == null) return null;
        return bio.length() <= 160 ? bio : bio.substring(0, bio.lastIndexOf(' ', 157) > 0 ? bio.lastIndexOf(' ', 157) : 157) + "…";
    }

    /** Minúsculas e sem acentos, para "matematica" encontrar "Matemática" e "sao luis" encontrar "São Luís". */
    static String normalizar(String s) {
        if (s == null || s.isBlank()) return null;
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
