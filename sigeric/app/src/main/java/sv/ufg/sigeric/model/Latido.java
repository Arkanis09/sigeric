package sv.ufg.sigeric.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Escritura mínima que usan los scripts de prueba de alta disponibilidad. */
@Entity
@Table(name = "latido")
public class Latido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nodo_app")
    private String nodoApp;
    @Column(name = "nodo_bd")
    private String nodoBd;
    private LocalDateTime fecha;

    public Latido() {}
    public Latido(String nodoApp, String nodoBd) {
        this.nodoApp = nodoApp;
        this.nodoBd = nodoBd;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNodoApp() { return nodoApp; }
    public String getNodoBd() { return nodoBd; }
    public LocalDateTime getFecha() { return fecha; }
}
