package sv.ufg.sigeric.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.ufg.sigeric.model.Latido;

public interface LatidoRepo extends JpaRepository<Latido, Long> {
}
