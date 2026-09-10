package bulktrack.service;

import bulktrack.dto.PerfilRequest;
import bulktrack.dto.PerfilResponse;
import bulktrack.entity.Perfil;
import bulktrack.repository.PerfilRepository;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {
    private final PerfilRepository perfilRepository;

    public PerfilService(PerfilRepository perfilRepository){
        this.perfilRepository = perfilRepository;
    }

    public PerfilResponse criarPerfil(PerfilRequest request){
        Perfil perfil = new Perfil(
                request.getNome(),
                request.getDataNascimento(),
                request.getSexo(),
                request.getAltura(),
                request.getPesoAtual(),
                request.getPercentualObjetivo(),
                request.getNivelAtividade(),
                request.getObjetivo()
        );
        Perfil perfilSalvo = perfilRepository.save(perfil);

        PerfilResponse response = new PerfilResponse();

        response.setId(perfilSalvo.getId());
        response.setNome(perfilSalvo.getNome());
        response.setDataNascimento(perfilSalvo.getDataNascimento());
        response.setAltura(perfilSalvo.getAltura());
        response.setPesoAtual(perfilSalvo.getPesoAtual());
        response.setPercentualObjetivo(perfilSalvo.getPercentualObjetivo());
        response.setNivelAtividade(perfilSalvo.getNivelAtividade());
        response.setObjetivo(perfilSalvo.getObjetivo());

        return response;
    }
}
