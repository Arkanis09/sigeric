package sv.ufg.sigeric.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página en vivo del estado de alta disponibilidad (se refresca sola con /api/estado). */
@Controller
public class EstadoController {

    @GetMapping("/estado")
    public String estado() {
        return "estado";
    }
}
