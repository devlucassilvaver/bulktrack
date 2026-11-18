package bulktrack.controller;

import bulktrack.dto.Treino;
import bulktrack.service.TreinoService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfis/{perfil}/treinos")
public class TreinoController {
    private final TreinoService treinos;

    public TreinoController(TreinoService treinos) {
        this.treinos = treinos;
    }

    @GetMapping
    public List<Treino.Registro> listar(Principal conta, @PathVariable long perfil,
                                      @RequestParam(required = false) LocalDate data) {
        return treinos.listar(conta.getName(), perfil, data);
    }

    @GetMapping("/{id}")
    public Treino.Registro buscar(Principal conta, @PathVariable long perfil, @PathVariable long id) {
        return treinos.buscar(conta.getName(), perfil, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Treino.Registro criar(Principal conta, @PathVariable long perfil, @Valid @RequestBody Treino.Sessao pedido) {
        return treinos.criar(conta.getName(), perfil, pedido);
    }

    @PutMapping("/{id}")
    public Treino.Registro atualizar(Principal conta, @PathVariable long perfil, @PathVariable long id,
                                     @Valid @RequestBody Treino.Sessao pedido) {
        return treinos.atualizar(conta.getName(), perfil, id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Principal conta, @PathVariable long perfil, @PathVariable long id) {
        treinos.excluir(conta.getName(), perfil, id);
    }
}
