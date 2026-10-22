package sv.ufg.sigeric.model;

public enum EstadoReporte {
    RECIBIDO("Recibido"), EN_REVISION("En revisión"), EN_PROCESO("En proceso"),
    RESUELTO("Resuelto"), RECHAZADO("Rechazado");

    private final String etiqueta;
    EstadoReporte(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
