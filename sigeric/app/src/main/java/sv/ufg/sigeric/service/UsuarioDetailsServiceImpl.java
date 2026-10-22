package sv.ufg.sigeric.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sv.ufg.sigeric.config.ReintentoBD;
import sv.ufg.sigeric.model.Usuario;
import sv.ufg.sigeric.repo.UsuarioRepo;

@Service
@ReintentoBD
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepo usuarios;

    public UsuarioDetailsServiceImpl(UsuarioRepo usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario u = usuarios.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(u.getEmail())
                .password(u.getPassword())
                .roles(u.getRol().name())
                .build();
    }
}
