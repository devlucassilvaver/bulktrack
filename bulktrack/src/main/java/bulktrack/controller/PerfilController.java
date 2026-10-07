package bulktrack.controller;

import bulktrack.dto.HistoricoPesoRequest;
import bulktrack.dto.PerfilRequest;
import bulktrack.dto.PerfilResponse;
import bulktrack.entity.Perfil;
import bulktrack.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/perfis")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService){
        this.perfilService = perfilService;
    }

    @PostMapping
    public ResponseEntity<PerfilResponse> criarPerfil(
            @Valid @RequestBody PerfilRequest perfilRequest){
        PerfilResponse perfil = perfilService.criarPerfil(perfilRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(perfil);
    }

    @PostMapping("{id}/pesos")
    public ResponseEntity<Void> registrarPeso(
            @PathVariable Long id,
            @Valid @RequestBody HistoricoPesoRequest request){

        perfilService.registrarPeso(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("{id}")
    public PerfilResponse buscarPorId(@PathVariable Long id){
        return perfilService.buscarPorId(id);
    }

    @GetMapping("{id}/tmb")
    public BigDecimal calcularTMB(@PathVariable Long id){
        return perfilService.calcularTMB(id);
    }

    @GetMapping("{id}/tmt")
    public BigDecimal calcularTMT(@PathVariable Long id){
        return perfilService.calcularTMT(id);
    }

    @GetMapping("{id}/calorias")
    public BigDecimal calcularMetaCalorica(@PathVariable Long id){
        return perfilService.calcularMetaCalorica(id);
    }

}
