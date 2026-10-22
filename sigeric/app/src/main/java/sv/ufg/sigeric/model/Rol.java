package sv.ufg.sigeric.model;

public enum Rol {
    CIUDADANO("Ciudadano"), ADESCO("Representante ADESCO"), MUNICIPAL("Personal municipal");

    private final String etiqueta;
    Rol(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
