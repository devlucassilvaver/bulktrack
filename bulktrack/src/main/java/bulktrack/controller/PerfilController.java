package bulktrack.controller;

import bulktrack.service.PerfilService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/perfis")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService){
        this.perfilService = perfilService;
    }
}
