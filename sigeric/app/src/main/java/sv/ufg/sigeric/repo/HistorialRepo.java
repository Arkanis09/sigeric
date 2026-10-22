package sv.ufg.sigeric.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.ufg.sigeric.model.HistorialEstado;
import sv.ufg.sigeric.model.Reporte;

import java.util.List;

public interface HistorialRepo extends JpaRepository<HistorialEstado, Long> {
    List<HistorialEstado> findByReporteOrderByFechaAsc(Reporte reporte);
}
