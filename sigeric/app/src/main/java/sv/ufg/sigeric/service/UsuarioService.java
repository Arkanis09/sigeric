package sv.ufg.sigeric.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ufg.sigeric.config.ReintentoBD;
import sv.ufg.sigeric.model.Usuario;
import sv.ufg.sigeric.repo.UsuarioRepo;

import java.util.Optional;

@Service
@ReintentoBD
public class UsuarioService {

    private final UsuarioRepo usuarios;

    public UsuarioService(UsuarioRepo usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> porEmail(String email) {
        return usuarios.findByEmail(email.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public boolean existe(String email) {
        return usuarios.existsByEmail(email);
    }

    @Transactional
    public Usuario guardar(Usuario u) {
        return usuarios.save(u);
    }
}
