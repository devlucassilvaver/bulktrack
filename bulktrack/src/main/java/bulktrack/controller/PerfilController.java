package bulktrack.controller;

import bulktrack.dto.*;
import bulktrack.service.PerfilService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfis")
public class PerfilController {
    private final PerfilService perfis;

    public PerfilController(PerfilService perfis) {
        this.perfis = perfis;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilResponse criar(Principal conta, @Valid @RequestBody PerfilRequest pedido) {
        return perfis.criarPerfil(conta.getName(), pedido);
    }

    @GetMapping("/me")
    public PerfilResponse meu(Principal conta) {
        return perfis.buscar(conta.getName(), null);
    }

    @GetMapping("/{id}")
    public PerfilResponse buscar(Principal conta, @PathVariable long id) {
        return perfis.buscar(conta.getName(), id);
    }

    @PutMapping("/{id}")
    public PerfilResponse atualizar(Principal conta, @PathVariable long id, @Valid @RequestBody PerfilRequest pedido) {
        return perfis.atualizar(conta.getName(), id, pedido);
    }

    @PostMapping("/{id}/pesos")
    @ResponseStatus(HttpStatus.CREATED)
    public void pesar(Principal conta, @PathVariable long id, @Valid @RequestBody HistoricoPesoRequest pedido) {
        perfis.registrarPeso(conta.getName(), id, pedido);
    }

    @GetMapping("/{id}/pesos")
    public List<PerfilService.Pesagem> pesos(Principal conta, @PathVariable long id) {
        return perfis.historico(conta.getName(), id);
    }

    @GetMapping("/{id}/tmb")
    public BigDecimal tmb(Principal conta, @PathVariable long id) {
        return buscar(conta, id).tmb();
    }

    @GetMapping("/{id}/tmt")
    public BigDecimal get(Principal conta, @PathVariable long id) {
        return buscar(conta, id).get();
    }

    @GetMapping("/{id}/calorias")
    public BigDecimal calorias(Principal conta, @PathVariable long id) {
        var meta = buscar(conta, id).meta();
        return meta == null ? null : meta.calorias();
    }
}
