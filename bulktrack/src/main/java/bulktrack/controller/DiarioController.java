package bulktrack.controller;

import bulktrack.dto.*;
import bulktrack.service.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class DiarioController {
    private final DiarioService diario;
    private final FoodDataCentral fonte;

    public DiarioController(DiarioService diario, FoodDataCentral fonte) {
        this.diario = diario;
        this.fonte = fonte;
    }

    @GetMapping("/api/alimentos")
    public List<Alimento> buscar(@RequestParam String q) {
        if (q.isBlank() || q.length() > 120) {
            throw new IllegalArgumentException("Informe um termo de busca de até 120 caracteres.");
        }
        return fonte.buscar(q);
    }

    public record Previa(@jakarta.validation.constraints.NotNull @Valid Nutrientes por100g,
                         @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive
                         @jakarta.validation.constraints.Digits(integer = 5, fraction = 2) BigDecimal gramas) {}

    @PostMapping("/api/alimentos/previa")
    public Nutrientes previa(@Valid @RequestBody Previa pedido) {
        return pedido.por100g().porcao(pedido.gramas());
    }

    @GetMapping("/perfis/{perfil}/diario")
    public DiarioService.Resumo resumo(Principal conta, @PathVariable long perfil,
                                       @RequestParam(required = false) LocalDate data) {
        return diario.resumo(conta.getName(), perfil, data);
    }

    @PostMapping("/perfis/{perfil}/diario")
    @ResponseStatus(HttpStatus.CREATED)
    public Consumo.Registro registrar(Principal conta, @PathVariable long perfil, @Valid @RequestBody Consumo.Novo pedido) {
        return diario.registrar(conta.getName(), perfil, pedido);
    }

    @PutMapping("/perfis/{perfil}/diario/{id}")
    public Consumo.Registro atualizar(Principal conta, @PathVariable long perfil, @PathVariable long id,
                                      @Valid @RequestBody Consumo.Quantidade pedido) {
        return diario.atualizar(conta.getName(), perfil, id, pedido);
    }

    @DeleteMapping("/perfis/{perfil}/diario/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Principal conta, @PathVariable long perfil, @PathVariable long id) {
        diario.excluir(conta.getName(), perfil, id);
    }
}
