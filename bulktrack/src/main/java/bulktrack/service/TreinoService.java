package bulktrack.service;

import bulktrack.dto.Treino;
import bulktrack.exception.PerfilNaoEncontradoException;
import bulktrack.repository.TreinoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TreinoService {
    private final PerfilService perfis;
    private final TreinoRepository treinos;

    public TreinoService(PerfilService perfis, TreinoRepository treinos) {
        this.perfis = perfis;
        this.treinos = treinos;
    }

    public Treino.Registro criar(String usuario, long perfil, Treino.Sessao sessao) {
        perfis.proprio(usuario, perfil);
        validar(sessao.data());
        return buscarRegistro(perfil, treinos.inserir(perfil, sessao));
    }

    public Treino.Registro atualizar(String usuario, long perfil, long id, Treino.Sessao sessao) {
        perfis.proprio(usuario, perfil);
        validar(sessao.data());
        treinos.atualizar(perfil, id, sessao);
        return buscarRegistro(perfil, id);
    }

    public void excluir(String usuario, long perfil, long id) {
        perfis.proprio(usuario, perfil);
        if (treinos.excluir(perfil, id) == 0) {
            throw ausente();
        }
    }

    @Transactional(readOnly = true)
    public List<Treino.Registro> listar(String usuario, long perfil, LocalDate data) {
        perfis.proprio(usuario, perfil);
        if (data != null) {
            validar(data);
        }
        return treinos.listar(perfil, data);
    }

    @Transactional(readOnly = true)
    public Treino.Registro buscar(String usuario, long perfil, long id) {
        perfis.proprio(usuario, perfil);
        return buscarRegistro(perfil, id);
    }

    private Treino.Registro buscarRegistro(long perfil, long id) {
        return treinos.buscar(perfil, id).orElseThrow(TreinoService::ausente);
    }

    private static PerfilNaoEncontradoException ausente() {
        return new PerfilNaoEncontradoException("Treino não encontrado.");
    }

    private static void validar(LocalDate data) {
        if (data.getYear() < 1900 || data.getYear() > 9998) {
            throw new IllegalArgumentException("Informe um ano entre 1900 e 9998.");
        }
    }
}
