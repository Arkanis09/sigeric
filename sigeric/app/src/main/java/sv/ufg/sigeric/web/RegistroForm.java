package sv.ufg.sigeric.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sv.ufg.sigeric.model.Rol;

public class RegistroForm {
    @NotBlank(message = "Escribe tu nombre") @Size(max = 120)
    private String nombre;
    @NotBlank(message = "Escribe tu correo") @Email(message = "Correo no válido") @Size(max = 120)
    private String email;
    @NotBlank @Size(min = 6, max = 60, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    @NotBlank(message = "Indica tu municipio") @Size(max = 80)
    private String municipio;
    @NotNull
    private Rol rol = Rol.CIUDADANO;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
