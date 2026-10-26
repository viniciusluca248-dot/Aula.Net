package br.uema.aulanet.service;

import br.uema.aulanet.dto.CadastroRequest;
import br.uema.aulanet.dto.PerfilRequest;
import br.uema.aulanet.dto.UsuarioDTO;
import br.uema.aulanet.exception.NaoEncontradoException;
import br.uema.aulanet.model.Papel;
import br.uema.aulanet.model.Usuario;
import br.uema.aulanet.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    public UsuarioService(UsuarioRepository usuarioRepository, AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscar(Integer id) {
        return UsuarioDTO.de(obter(id));
    }

    @Transactional
    public UsuarioDTO atualizarPerfil(Integer id, PerfilRequest req) {
        Usuario u = obter(id);
        u.setNome(req.nome().trim());
        u.setTelefone(req.telefone());
        u.setBio(req.bio());
        return UsuarioDTO.de(u);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll(Sort.by("papel", "nome")).stream().map(UsuarioDTO::de).toList();
    }

    /** Administrador pode cadastrar qualquer papel, inclusive outro ADMIN. */
    @Transactional
    public UsuarioDTO criarPorAdmin(CadastroRequest req) {
        return UsuarioDTO.de(authService.criarUsuario(req, req.papel()));
    }

    Usuario obter(Integer id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    Usuario obterProfessor(Integer id) {
        Usuario u = obter(id);
        if (u.getPapel() != Papel.PROFESSOR) {
            throw new NaoEncontradoException("Professor não encontrado.");
        }
        return u;
    }
}
