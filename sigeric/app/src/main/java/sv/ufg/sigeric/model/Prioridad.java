package sv.ufg.sigeric.model;

public enum Prioridad {
    BAJA("Baja"), MEDIA("Media"), ALTA("Alta");

    private final String etiqueta;
    Prioridad(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
