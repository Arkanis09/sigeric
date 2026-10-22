package sv.ufg.sigeric.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.ufg.sigeric.config.ReintentoBD;
import sv.ufg.sigeric.model.Latido;
import sv.ufg.sigeric.repo.LatidoRepo;

import java.util.LinkedHashMap;
import java.util.Map;

/** Escritura de prueba que usan los scripts de alta disponibilidad. */
@Service
@ReintentoBD
public class LatidoService {

    private final NodoInfo nodo;
    private final LatidoRepo latidos;

    public LatidoService(NodoInfo nodo, LatidoRepo latidos) {
        this.nodo = nodo;
        this.latidos = latidos;
    }

    @Transactional
    public Map<String, Object> registrar() {
        String nodoBd = nodo.nodoBd();
        Latido l = latidos.save(new Latido(nodo.nodoApp(), nodoBd));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", l.getId());
        r.put("nodoApp", l.getNodoApp());
        r.put("nodoBd", l.getNodoBd());
        r.put("totalLatidos", latidos.count());
        return r;
    }
}
