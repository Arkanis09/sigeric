package sv.ufg.sigeric.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reporte")
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codigo;
    @Enumerated(EnumType.STRING)
    private Categoria categoria;
    private String titulo;
    private String descripcion;
    private String direccion;
    private String municipio;
    @Enumerated(EnumType.STRING)
    private EstadoReporte estado;
    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;
    private LocalDateTime creado;
    private LocalDateTime actualizado;
    @Column(name = "nodo_app")
    private String nodoApp;
    @Column(name = "nodo_bd")
    private String nodoBd;
    @OneToMany(mappedBy = "reporte", fetch = FetchType.LAZY)
    @OrderBy("fecha ASC")
    private List<HistorialEstado> historial = new ArrayList<>();

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public LocalDateTime getCreado() { return creado; }
    public void setCreado(LocalDateTime creado) { this.creado = creado; }
    public LocalDateTime getActualizado() { return actualizado; }
    public void setActualizado(LocalDateTime actualizado) { this.actualizado = actualizado; }
    public String getNodoApp() { return nodoApp; }
    public void setNodoApp(String nodoApp) { this.nodoApp = nodoApp; }
    public String getNodoBd() { return nodoBd; }
    public void setNodoBd(String nodoBd) { this.nodoBd = nodoBd; }
    public List<HistorialEstado> getHistorial() { return historial; }
}
