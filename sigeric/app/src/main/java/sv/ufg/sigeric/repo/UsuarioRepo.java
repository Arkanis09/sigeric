package sv.ufg.sigeric.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.ufg.sigeric.model.Usuario;

import java.util.Optional;

public interface UsuarioRepo extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
