package sv.ufg.sigeric.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sv.ufg.sigeric.model.*;

import java.util.List;
import java.util.Optional;

public interface ReporteRepo extends JpaRepository<Reporte, Long> {
    Optional<Reporte> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Reporte> findByAutorOrderByCreadoDesc(Usuario autor);
    List<Reporte> findAllByOrderByCreadoDesc();
    List<Reporte> findByEstadoOrderByCreadoDesc(EstadoReporte estado);
    List<Reporte> findByCategoriaOrderByCreadoDesc(Categoria categoria);
    List<Reporte> findByEstadoAndCategoriaOrderByCreadoDesc(EstadoReporte estado, Categoria categoria);
    long countByEstado(EstadoReporte estado);

    @Query("select r.categoria, count(r) from Reporte r group by r.categoria")
    List<Object[]> contarPorCategoria();

    @Query("select r.estado, count(r) from Reporte r group by r.estado")
    List<Object[]> contarPorEstado();
}
