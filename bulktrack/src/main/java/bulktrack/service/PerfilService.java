package bulktrack.service;

import bulktrack.dto.PerfilRequest;
import bulktrack.dto.PerfilResponse;
import bulktrack.entity.Perfil;
import bulktrack.enums.Sexo;
import bulktrack.exception.PerfilNaoEncontradoException;
import bulktrack.repository.PerfilRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Service
public class PerfilService {
    private final PerfilRepository perfilRepository;

    public PerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    public PerfilResponse criarPerfil(PerfilRequest request) {
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
        response.setSexo(perfilSalvo.getSexo());
        response.setAltura(perfilSalvo.getAltura());
        response.setPesoAtual(perfilSalvo.getPesoAtual());
        response.setPercentualObjetivo(perfilSalvo.getPercentualObjetivo());
        response.setNivelAtividade(perfilSalvo.getNivelAtividade());
        response.setObjetivo(perfilSalvo.getObjetivo());

        return response;
    }

    public PerfilResponse buscarPorId(Long id) {
        Perfil perfil = perfilRepository
                .findById(id)
                .orElseThrow(() -> new PerfilNaoEncontradoException(
                        "Perfil não encontrado"
                ));

        PerfilResponse response = new PerfilResponse();
        response.setId(perfil.getId());
        response.setNome(perfil.getNome());
        response.setDataNascimento(perfil.getDataNascimento());
        response.setSexo(perfil.getSexo());
        response.setAltura(perfil.getAltura());
        response.setPesoAtual(perfil.getPesoAtual());
        response.setPercentualObjetivo(perfil.getPercentualObjetivo());
        response.setNivelAtividade(perfil.getNivelAtividade());
        response.setObjetivo(perfil.getObjetivo());

        return response;
    }

    public BigDecimal calcularTMB(Long id) {
        Perfil perfil = buscarEntidadePorId(id);

        var anoHoje = LocalDate.now();
        var dataNascimento = perfil.getDataNascimento();
        Period idade = Period.between(dataNascimento, anoHoje);

        BigDecimal alturaCm = perfil.getAltura()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal peso = perfil.getPesoAtual();
        BigDecimal idadeAnos = BigDecimal.valueOf(idade.getYears());

        BigDecimal tmbBase = BigDecimal.valueOf(10).multiply(peso)
                .add(BigDecimal.valueOf(6.25).multiply(alturaCm))
                .subtract(BigDecimal.valueOf(5).multiply(idadeAnos));

        if (perfil.getSexo() == Sexo.MASCULINO) {
            return tmbBase.add(BigDecimal.valueOf(5));
        } else if (perfil.getSexo() == Sexo.FEMININO) {
            return tmbBase.subtract(BigDecimal.valueOf(161));
        }

        throw new PerfilNaoEncontradoException(
                "Perfil não encontrado"
        );

    }

    private Perfil buscarEntidadePorId(Long id){
        Perfil busca = perfilRepository
                .findById(id)
                .orElseThrow(() -> new PerfilNaoEncontradoException(
                                "Perfil não encontrado."
                        )
                );
        return busca;
    }
}
