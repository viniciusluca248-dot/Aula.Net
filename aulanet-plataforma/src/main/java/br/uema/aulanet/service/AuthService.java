package br.uema.aulanet.service;

import br.uema.aulanet.dto.AuthResponse;
import br.uema.aulanet.dto.CadastroRequest;
import br.uema.aulanet.dto.LoginRequest;
import br.uema.aulanet.dto.UsuarioDTO;
import br.uema.aulanet.exception.RegraNegocioException;
import br.uema.aulanet.model.Papel;
import br.uema.aulanet.model.Usuario;
import br.uema.aulanet.repository.UsuarioRepository;
import br.uema.aulanet.security.JwtService;
import br.uema.aulanet.security.UsuarioLogado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Cadastro público: somente ALUNO ou PROFESSOR. Contas ADMIN são criadas por outro administrador. */
    @Transactional
    public AuthResponse cadastrar(CadastroRequest req) {
        if (req.papel() == Papel.ADMIN) {
            throw new RegraNegocioException("Contas de administrador só podem ser criadas por um administrador.");
        }
        Usuario usuario = criarUsuario(req, req.papel());
        return gerarResposta(new UsuarioLogado(usuario));
    }

    @Transactional
    public Usuario criarUsuario(CadastroRequest req, Papel papel) {
        String email = req.email().trim().toLowerCase();
        String cpf = somenteDigitos(req.cpf());
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um usuário com este e-mail.");
        }
        if (cpf != null && usuarioRepository.existsByCpf(cpf)) {
            throw new RegraNegocioException("Já existe um usuário com este CPF.");
        }
        Usuario u = new Usuario(req.nome().trim(), email, passwordEncoder.encode(req.senha()), cpf, req.telefone(), papel);
        u.setBio(req.bio());
        return usuarioRepository.save(u);
    }

    public AuthResponse login(LoginRequest req) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.senha()));
        return gerarResposta((UsuarioLogado) auth.getPrincipal());
    }

    private AuthResponse gerarResposta(UsuarioLogado logado) {
        String token = jwtService.gerarToken(logado);
        Usuario u = usuarioRepository.findById(logado.getId()).orElseThrow();
        return new AuthResponse(token, jwtService.expiracao(token), UsuarioDTO.de(u));
    }

    static String somenteDigitos(String cpf) {
        if (cpf == null) return null;
        String d = cpf.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }
}
