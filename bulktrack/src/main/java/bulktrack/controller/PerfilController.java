package bulktrack.controller;

import bulktrack.dto.PerfilRequest;
import bulktrack.dto.PerfilResponse;
import bulktrack.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
