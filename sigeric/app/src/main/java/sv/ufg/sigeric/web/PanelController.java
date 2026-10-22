package sv.ufg.sigeric.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sv.ufg.sigeric.service.ReporteService;

/** Panel de priorización para el personal municipal. */
@Controller
public class PanelController {

    private final ReporteService reportes;

    public PanelController(ReporteService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("stats", reportes.estadisticas());
        return "panel";
    }
}
