package bulktrack.repository;

import bulktrack.dto.Treino;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class TreinoRepository {
    private static final TypeReference<List<Treino.Exercicio>> EXERCICIOS = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    public TreinoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long inserir(long perfil, Treino.Sessao sessao) {
        var chaves = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            var stmt = conexao.prepareStatement("""
                    INSERT INTO treino (perfil_id, data, nome, exercicios) VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, perfil);
            stmt.setObject(2, sessao.data());
            stmt.setString(3, sessao.nome());
            stmt.setString(4, json.writeValueAsString(sessao.exercicios()));
            return stmt;
        }, chaves);
        return Objects.requireNonNull(chaves.getKey()).longValue();
    }

    public void atualizar(long perfil, long id, Treino.Sessao sessao) {
        jdbc.update("UPDATE treino SET data = ?, nome = ?, exercicios = ? WHERE perfil_id = ? AND id = ?",
                sessao.data(), sessao.nome(), json.writeValueAsString(sessao.exercicios()), perfil, id);
    }

    public Optional<Treino.Registro> buscar(long perfil, long id) {
        return jdbc.query("SELECT * FROM treino WHERE perfil_id = ? AND id = ?", this::mapear, perfil, id)
                .stream().findFirst();
    }

    public List<Treino.Registro> listar(long perfil, LocalDate data) {
        if (data == null) {
            return jdbc.query("SELECT * FROM treino WHERE perfil_id = ? ORDER BY data DESC, id DESC",
                    this::mapear, perfil);
        }
        return jdbc.query("SELECT * FROM treino WHERE perfil_id = ? AND data = ? ORDER BY id DESC",
                this::mapear, perfil, data);
    }

    public int excluir(long perfil, long id) {
        return jdbc.update("DELETE FROM treino WHERE perfil_id = ? AND id = ?", perfil, id);
    }

    private Treino.Registro mapear(ResultSet rs, int linha) throws SQLException {
        return new Treino.Registro(rs.getLong("id"), rs.getObject("data", LocalDate.class), rs.getString("nome"),
                json.readValue(rs.getString("exercicios"), EXERCICIOS));
    }
}
