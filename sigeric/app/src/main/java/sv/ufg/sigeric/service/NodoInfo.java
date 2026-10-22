package sv.ufg.sigeric.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Información de alta disponibilidad: qué instancia de la app atiende y a qué nodo
 * del clúster Galera está conectada, más el estado del clúster.
 */
@Service
public class NodoInfo {

    private final JdbcTemplate jdbc;
    private final String nodoApp;

    public NodoInfo(JdbcTemplate jdbc, @Value("${sigeric.nodo-app}") String nodoApp) {
        this.jdbc = jdbc;
        this.nodoApp = nodoApp;
    }

    public String nodoApp() {
        return nodoApp;
    }

    /** Nodo Galera de la conexión actual (dentro de una transacción es el que recibe la escritura). */
    public String nodoBd() {
        try {
            return jdbc.queryForObject("SELECT @@wsrep_node_name", String.class);
        } catch (Exception e) {
            return "sin conexión";
        }
    }

    public Map<String, Object> estado() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("nodoApp", nodoApp);
        try {
            r.put("nodoBd", jdbc.queryForObject("SELECT @@wsrep_node_name", String.class));
            jdbc.query("SHOW STATUS WHERE Variable_name IN ('wsrep_cluster_size','wsrep_cluster_status'," +
                            "'wsrep_local_state_comment','wsrep_ready','wsrep_incoming_addresses','wsrep_last_committed')",
                    rs -> { r.put(rs.getString(1), rs.getString(2)); });
            r.put("ok", true);
        } catch (Exception e) {
            r.put("ok", false);
            r.put("error", e.getMessage());
        }
        return r;
    }
}
