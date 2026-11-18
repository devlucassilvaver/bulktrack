package bulktrack.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class SessaoController {
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        token.getToken();
        return token;
    }
}
