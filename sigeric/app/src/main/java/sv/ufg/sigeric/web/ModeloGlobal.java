package sv.ufg.sigeric.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sv.ufg.sigeric.model.Categoria;
import sv.ufg.sigeric.model.EstadoReporte;
import sv.ufg.sigeric.model.Prioridad;
import sv.ufg.sigeric.service.NodoInfo;

/** Datos disponibles en todas las vistas, incluida la barra de alta disponibilidad del pie de página. */
@ControllerAdvice(basePackages = "sv.ufg.sigeric.web")
public class ModeloGlobal {

    private final NodoInfo nodo;

    public ModeloGlobal(NodoInfo nodo) {
        this.nodo = nodo;
    }

    @ModelAttribute("nodoApp")
    public String nodoApp() { return nodo.nodoApp(); }

    @ModelAttribute("nodoBd")
    public String nodoBd() { return nodo.nodoBd(); }

    @ModelAttribute("categorias")
    public Categoria[] categorias() { return Categoria.values(); }

    @ModelAttribute("estados")
    public EstadoReporte[] estados() { return EstadoReporte.values(); }

    @ModelAttribute("prioridades")
    public Prioridad[] prioridades() { return Prioridad.values(); }
}
