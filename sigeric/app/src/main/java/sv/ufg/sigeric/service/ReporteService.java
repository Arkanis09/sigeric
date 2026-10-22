package sv.ufg.sigeric.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ufg.sigeric.config.ReintentoBD;
import sv.ufg.sigeric.model.*;
import sv.ufg.sigeric.repo.HistorialRepo;
import sv.ufg.sigeric.repo.ReporteRepo;
import sv.ufg.sigeric.web.ReporteForm;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
@ReintentoBD
public class ReporteService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReporteRepo reportes;
    private final HistorialRepo historial;
    private final NodoInfo nodo;

    public ReporteService(ReporteRepo reportes, HistorialRepo historial, NodoInfo nodo) {
        this.reportes = reportes;
        this.historial = historial;
        this.nodo = nodo;
    }

    @Transactional
    public Reporte crear(ReporteForm form, Usuario autor) {
        LocalDateTime ahora = LocalDateTime.now();
        String nodoBd = nodo.nodoBd();

        Reporte r = new Reporte();
        r.setCodigo(generarCodigo());
        r.setCategoria(form.getCategoria());
        r.setTitulo(form.getTitulo().trim());
        r.setDescripcion(form.getDescripcion().trim());
        r.setDireccion(form.getDireccion().trim());
        r.setMunicipio(form.getMunicipio().trim());
        r.setEstado(EstadoReporte.RECIBIDO);
        r.setPrioridad(Prioridad.MEDIA);
        r.setAutor(autor);
        r.setCreado(ahora);
        r.setActualizado(ahora);
        r.setNodoApp(nodo.nodoApp());
        r.setNodoBd(nodoBd);
        reportes.save(r);

        registrarHistorial(r, null, EstadoReporte.RECIBIDO, "Reporte creado", autor, nodoBd);
        return r;
    }

    @Transactional
    public void cambiarEstado(String codigo, EstadoReporte nuevo, Prioridad prioridad, String comentario, Usuario usuario) {
        Reporte r = buscar(codigo).orElseThrow();
        if (!puedeCambiarA(usuario.getRol(), nuevo)) {
            throw new AccessDeniedException("Tu rol no puede poner el reporte en estado " + nuevo.getEtiqueta());
        }
        EstadoReporte anterior = r.getEstado();
        r.setEstado(nuevo);
        if (prioridad != null && usuario.getRol() == Rol.MUNICIPAL) {
            r.setPrioridad(prioridad);
        }
        r.setActualizado(LocalDateTime.now());
        String texto = (comentario == null || comentario.isBlank()) ? null : comentario.trim();
        registrarHistorial(r, anterior, nuevo, texto, usuario, nodo.nodoBd());
    }

    /** ADESCO valida o rechaza reportes de su comunidad; el personal municipal gestiona todo el ciclo. */
    public List<EstadoReporte> estadosPermitidos(Rol rol) {
        return switch (rol) {
            case MUNICIPAL -> List.of(EstadoReporte.values());
            case ADESCO -> List.of(EstadoReporte.EN_REVISION, EstadoReporte.RECHAZADO);
            case CIUDADANO -> List.of();
        };
    }

    public boolean puedeCambiarA(Rol rol, EstadoReporte estado) {
        return estadosPermitidos(rol).contains(estado);
    }

    @Transactional(readOnly = true)
    public Optional<Reporte> buscar(String codigo) {
        return reportes.findByCodigo(codigo.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<HistorialEstado> historialDe(Reporte r) {
        return historial.findByReporteOrderByFechaAsc(r);
    }

    @Transactional(readOnly = true)
    public List<Reporte> listar(Usuario usuario, EstadoReporte estado, Categoria categoria) {
        if (usuario.getRol() == Rol.CIUDADANO) {
            return reportes.findByAutorOrderByCreadoDesc(usuario);
        }
        if (estado != null && categoria != null) return reportes.findByEstadoAndCategoriaOrderByCreadoDesc(estado, categoria);
        if (estado != null) return reportes.findByEstadoOrderByCreadoDesc(estado);
        if (categoria != null) return reportes.findByCategoriaOrderByCreadoDesc(categoria);
        return reportes.findAllByOrderByCreadoDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> estadisticas() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", reportes.count());
        m.put("resueltos", reportes.countByEstado(EstadoReporte.RESUELTO));
        m.put("pendientes", reportes.count() - reportes.countByEstado(EstadoReporte.RESUELTO)
                - reportes.countByEstado(EstadoReporte.RECHAZADO));

        Map<Categoria, Long> porCategoria = new EnumMap<>(Categoria.class);
        for (Categoria c : Categoria.values()) porCategoria.put(c, 0L);
        reportes.contarPorCategoria().forEach(f -> porCategoria.put((Categoria) f[0], (Long) f[1]));
        m.put("porCategoria", porCategoria);

        Map<EstadoReporte, Long> porEstado = new EnumMap<>(EstadoReporte.class);
        for (EstadoReporte e : EstadoReporte.values()) porEstado.put(e, 0L);
        reportes.contarPorEstado().forEach(f -> porEstado.put((EstadoReporte) f[0], (Long) f[1]));
        m.put("porEstado", porEstado);
        return m;
    }

    private void registrarHistorial(Reporte r, EstadoReporte anterior, EstadoReporte nuevo,
                                    String comentario, Usuario usuario, String nodoBd) {
        HistorialEstado h = new HistorialEstado();
        h.setReporte(r);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setComentario(comentario);
        h.setUsuario(usuario);
        h.setFecha(LocalDateTime.now());
        h.setNodoApp(nodo.nodoApp());
        h.setNodoBd(nodoBd);
        historial.save(h);
    }

    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("SIG-").append(Year.now().getValue()).append('-');
            for (int i = 0; i < 6; i++) sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
            codigo = sb.toString();
        } while (reportes.existsByCodigo(codigo));
        return codigo;
    }
}
