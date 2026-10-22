package sv.ufg.sigeric.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estado")
public class HistorialEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporte_id")
    private Reporte reporte;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoReporte estadoAnterior;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo")
    private EstadoReporte estadoNuevo;
    private String comentario;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    private LocalDateTime fecha;
    @Column(name = "nodo_app")
    private String nodoApp;
    @Column(name = "nodo_bd")
    private String nodoBd;

    public Long getId() { return id; }
    public Reporte getReporte() { return reporte; }
    public void setReporte(Reporte reporte) { this.reporte = reporte; }
    public EstadoReporte getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoReporte estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoReporte getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoReporte estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getNodoApp() { return nodoApp; }
    public void setNodoApp(String nodoApp) { this.nodoApp = nodoApp; }
    public String getNodoBd() { return nodoBd; }
    public void setNodoBd(String nodoBd) { this.nodoBd = nodoBd; }
}
