package sv.ufg.sigeric.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.ufg.sigeric.service.LatidoService;
import sv.ufg.sigeric.service.NodoInfo;

import java.util.Map;

/**
 * Endpoints que usan los scripts de prueba (scripts/prueba-ha-*.sh) para demostrar
 * que el sistema sigue leyendo y ESCRIBIENDO mientras se apagan nodos.
 */
@RestController
@RequestMapping("/api")
public class AltaDisponibilidadApi {

    private final NodoInfo nodo;
    private final LatidoService latidos;

    public AltaDisponibilidadApi(NodoInfo nodo, LatidoService latidos) {
        this.nodo = nodo;
        this.latidos = latidos;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return nodo.estado();
    }

    /** Inserta una fila y devuelve qué instancia de la app y qué nodo Galera la procesaron. */
    @PostMapping("/latido")
    public Map<String, Object> latido() {
        return latidos.registrar();
    }
}
