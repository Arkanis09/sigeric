package sv.ufg.sigeric.web;

import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sv.ufg.sigeric.model.*;
import sv.ufg.sigeric.service.ReporteService;
import sv.ufg.sigeric.service.UsuarioService;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reportes;
    private final UsuarioService usuarios;

    public ReporteController(ReporteService reportes, UsuarioService usuarios) {
        this.reportes = reportes;
        this.usuarios = usuarios;
    }

    private Usuario actual(Authentication auth) {
        return usuarios.porEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String listar(@RequestParam(required = false) EstadoReporte estado,
                         @RequestParam(required = false) Categoria categoria,
                         Authentication auth, Model model) {
        Usuario u = actual(auth);
        model.addAttribute("usuario", u);
        model.addAttribute("reportes", reportes.listar(u, estado, categoria));
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroCategoria", categoria);
        return "reportes";
    }

    @GetMapping("/nuevo")
    public String nuevo(Authentication auth, Model model) {
        ReporteForm form = new ReporteForm();
        form.setMunicipio(actual(auth).getMunicipio());
        model.addAttribute("form", form);
        return "nuevo";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute("form") ReporteForm form, BindingResult br,
                        Authentication auth, RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "nuevo";
        }
        Reporte r = reportes.crear(form, actual(auth));
        ra.addFlashAttribute("mensaje", "Reporte registrado con el código " + r.getCodigo()
                + ". Guárdalo para consultar su avance.");
        return "redirect:/reportes/" + r.getCodigo();
    }

    @GetMapping("/{codigo}")
    public String detalle(@PathVariable String codigo, Authentication auth, Model model) {
        Usuario u = actual(auth);
        Reporte r = reportes.buscar(codigo).orElse(null);
        if (r == null) {
            model.addAttribute("codigo", codigo);
            return "no-encontrado";
        }
        if (u.getRol() == Rol.CIUDADANO && !r.getAutor().getId().equals(u.getId())) {
            throw new AccessDeniedException("Solo puedes ver tus propios reportes");
        }
        model.addAttribute("usuario", u);
        model.addAttribute("reporte", r);
        model.addAttribute("historial", reportes.historialDe(r));
        model.addAttribute("estadosPermitidos", reportes.estadosPermitidos(u.getRol()));
        return "detalle";
    }

    @PostMapping("/{codigo}/estado")
    public String cambiarEstado(@PathVariable String codigo,
                                @RequestParam EstadoReporte estado,
                                @RequestParam(required = false) Prioridad prioridad,
                                @RequestParam(required = false) String comentario,
                                Authentication auth, RedirectAttributes ra) {
        reportes.cambiarEstado(codigo, estado, prioridad, comentario, actual(auth));
        ra.addFlashAttribute("mensaje", "Estado actualizado a: " + estado.getEtiqueta());
        return "redirect:/reportes/" + codigo;
    }
}
