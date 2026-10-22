package sv.ufg.sigeric.model;

public enum Categoria {
    BACHE("Bache / calle dañada"), ALUMBRADO("Alumbrado público"), FUGA("Fuga de agua"), OTRO("Otro");

    private final String etiqueta;
    Categoria(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
