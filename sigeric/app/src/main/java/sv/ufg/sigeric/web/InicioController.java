package sv.ufg.sigeric.web;

import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sv.ufg.sigeric.model.Reporte;
import sv.ufg.sigeric.model.Rol;
import sv.ufg.sigeric.model.Usuario;
import sv.ufg.sigeric.service.ReporteService;
import sv.ufg.sigeric.service.UsuarioService;

@Controller
public class InicioController {

    private final ReporteService reportes;
    private final UsuarioService usuarios;
    private final PasswordEncoder encoder;

    public InicioController(ReporteService reportes, UsuarioService usuarios, PasswordEncoder encoder) {
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("stats", reportes.estadisticas());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("form", new RegistroForm());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("form") RegistroForm form, BindingResult br,
                            RedirectAttributes ra) {
        String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        if (!br.hasFieldErrors("email") && usuarios.existe(email)) {
            br.rejectValue("email", "duplicado", "Ya existe una cuenta con ese correo");
        }
        if (form.getRol() == Rol.MUNICIPAL) {
            br.rejectValue("rol", "rol", "Las cuentas municipales las crea la alcaldía");
        }
        if (br.hasErrors()) {
            return "registro";
        }
        Usuario u = new Usuario();
        u.setNombre(form.getNombre().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(form.getPassword()));
        u.setMunicipio(form.getMunicipio().trim());
        u.setRol(form.getRol());
        usuarios.guardar(u);
        ra.addFlashAttribute("mensaje", "Cuenta creada. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    /** Consulta pública: cualquier persona con el código puede ver el avance de un reporte. */
    @GetMapping("/seguimiento")
    public String seguimiento(@RequestParam(required = false) String codigo, Model model) {
        if (codigo != null && !codigo.isBlank()) {
            model.addAttribute("codigo", codigo.trim().toUpperCase());
            reportes.buscar(codigo).ifPresentOrElse(r -> {
                model.addAttribute("reporte", r);
                model.addAttribute("historial", reportes.historialDe(r));
            }, () -> model.addAttribute("noEncontrado", true));
        }
        return "seguimiento";
    }
}
