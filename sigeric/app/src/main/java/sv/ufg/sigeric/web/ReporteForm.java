package sv.ufg.sigeric.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sv.ufg.sigeric.model.Categoria;

public class ReporteForm {
    @NotNull(message = "Selecciona una categoría")
    private Categoria categoria;
    @NotBlank(message = "Escribe un título") @Size(max = 150)
    private String titulo;
    @NotBlank(message = "Describe el problema") @Size(max = 2000)
    private String descripcion;
    @NotBlank(message = "Indica la dirección o punto de referencia") @Size(max = 200)
    private String direccion;
    @NotBlank(message = "Indica el municipio") @Size(max = 80)
    private String municipio;

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
}
