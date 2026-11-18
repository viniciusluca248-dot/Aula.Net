package br.uema.aulanet.service;

import br.uema.aulanet.dto.MeuPerfilDTO;
import br.uema.aulanet.dto.PerfilRequest;
import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.FotoUsuario;
import br.uema.aulanet.model.Papel;
import br.uema.aulanet.model.PerfilUsuario;
import br.uema.aulanet.model.Usuario;
import br.uema.aulanet.repository.FotoUsuarioRepository;
import br.uema.aulanet.repository.OfertaAulaRepository;
import br.uema.aulanet.repository.PerfilUsuarioRepository;
import br.uema.aulanet.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

/** Perfil do usuário: dados pessoais, currículo do professor e foto. */
@Service
public class PerfilService {

    static final int TAMANHO_MAXIMO_FOTO = 2 * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final FotoUsuarioRepository fotoRepository;
    private final OfertaAulaRepository ofertaRepository;
    private final Clock clock;

    public PerfilService(UsuarioRepository usuarioRepository, PerfilUsuarioRepository perfilRepository,
                         FotoUsuarioRepository fotoRepository, OfertaAulaRepository ofertaRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.fotoRepository = fotoRepository;
        this.ofertaRepository = ofertaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MeuPerfilDTO meuPerfil(Integer usuarioId) {
        Usuario u = obterUsuario(usuarioId);
        return paraDTO(u, perfilRepository.findById(usuarioId).orElse(null));
    }

    @Transactional
    public MeuPerfilDTO atualizar(Integer usuarioId, PerfilRequest req) {
        Usuario u = obterUsuario(usuarioId);
        u.setNome(req.nome().trim());
        u.setTelefone(vazioParaNulo(req.telefone()));
        u.setBio(vazioParaNulo(req.bio()));
        PerfilUsuario p = perfilDe(u);
        p.setCidade(vazioParaNulo(req.cidade()));
        p.setUf(req.uf() == null || req.uf().isBlank() ? null : req.uf().trim().toUpperCase(Locale.ROOT));
        p.setFormacao(vazioParaNulo(req.formacao()));
        p.setExperiencia(vazioParaNulo(req.experiencia()));
        p.setLinkCurriculo(vazioParaNulo(req.linkCurriculo()));
        return paraDTO(u, p);
    }

    /** Aceita JPEG, PNG ou WebP de até 2 MB; o tipo é conferido pelos primeiros bytes, não pelo nome do arquivo. */
    @Transactional
    public MeuPerfilDTO salvarFoto(Integer usuarioId, byte[] conteudo) {
        Usuario u = obterUsuario(usuarioId);
        if (conteudo == null || conteudo.length == 0) {
            throw new RegraNegocioException("Selecione uma imagem.");
        }
        if (conteudo.length > TAMANHO_MAXIMO_FOTO) {
            throw new RegraNegocioException("A foto deve ter no máximo 2 MB.");
        }
        String tipo = tipoDaImagem(conteudo);
        if (tipo == null) {
            throw new RegraNegocioException("Formato não suportado: envie uma imagem JPEG, PNG ou WebP.");
        }
        FotoUsuario foto = fotoRepository.findById(usuarioId).orElseGet(() -> new FotoUsuario(usuarioId, tipo, conteudo));
        foto.setTipo(tipo);
        foto.setConteudo(conteudo);
        fotoRepository.save(foto);
        PerfilUsuario p = perfilDe(u);
        p.setFotoVersao(clock.millis());
        return paraDTO(u, p);
    }

    @Transactional
    public MeuPerfilDTO removerFoto(Integer usuarioId) {
        Usuario u = obterUsuario(usuarioId);
        fotoRepository.findById(usuarioId).ifPresent(fotoRepository::delete);
        PerfilUsuario p = perfilDe(u);
        p.setFotoVersao(null);
        return paraDTO(u, p);
    }

    @Transactional(readOnly = true)
    public FotoUsuario foto(Integer usuarioId) {
        return fotoRepository.findById(usuarioId).orElseThrow(() -> new NaoEncontradoException("Foto não encontrada."));
    }

    @Transactional(readOnly = true)
    public String fotoUrl(Integer usuarioId) {
        return perfilRepository.findById(usuarioId).map(PerfilService::fotoUrl).orElse(null);
    }

    public static String fotoUrl(PerfilUsuario p) {
        return p == null || p.getFotoVersao() == null ? null
                : "/api/usuarios/" + p.getId() + "/foto?v=" + p.getFotoVersao();
    }

    static String tipoDaImagem(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private MeuPerfilDTO paraDTO(Usuario u, PerfilUsuario p) {
        // completude: o que um aluno procura ao escolher um professor (ou o básico de contato, para o aluno)
        Map<String, Boolean> itens = new LinkedHashMap<>();
        itens.put("foto de perfil", p != null && p.getFotoVersao() != null);
        itens.put("telefone", u.getTelefone() != null);
        itens.put("cidade", p != null && p.getCidade() != null);
        if (u.getPapel() == Papel.PROFESSOR) {
            itens.put("apresentação (bio)", u.getBio() != null && u.getBio().length() >= 40);
            itens.put("formação", p != null && p.getFormacao() != null);
            itens.put("experiência", p != null && p.getExperiencia() != null);
            itens.put("ao menos um serviço cadastrado", !ofertaRepository.findByProfessorId(u.getId()).isEmpty());
        }
        List<String> faltando = itens.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).toList();
        int completude = Math.round(100f * (itens.size() - faltando.size()) / itens.size());
        return new MeuPerfilDTO(u.getId(), u.getNome(), u.getEmail(), u.getCpf(), u.getTelefone(), u.getPapel(),
                u.getBio(), p == null ? null : p.getCidade(), p == null ? null : p.getUf(),
                p == null ? null : p.getFormacao(), p == null ? null : p.getExperiencia(),
                p == null ? null : p.getLinkCurriculo(), fotoUrl(p), completude, faltando);
    }

    private PerfilUsuario perfilDe(Usuario u) {
        return perfilRepository.findById(u.getId()).orElseGet(() -> perfilRepository.save(new PerfilUsuario(u)));
    }

    private Usuario obterUsuario(Integer id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
