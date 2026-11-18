package bulktrack.repository;

import bulktrack.dto.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ConsumoRepository {
    private final JdbcTemplate jdbc;

    public ConsumoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long inserir(long perfil, Alimento alimento, BigDecimal gramas, long instante) {
        var chaves = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            var stmt = conexao.prepareStatement("""
                    INSERT INTO consumo (perfil_id, nome, fonte, calorias, carboidratos, proteinas,
                                         lipidios, gramas, instante) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            var n = alimento.por100g();
            stmt.setLong(1, perfil);
            stmt.setString(2, alimento.nome());
            stmt.setString(3, alimento.fonte());
            stmt.setBigDecimal(4, n.calorias());
            stmt.setBigDecimal(5, n.carboidratos());
            stmt.setBigDecimal(6, n.proteinas());
            stmt.setBigDecimal(7, n.lipidios());
            stmt.setBigDecimal(8, gramas);
            stmt.setLong(9, instante);
            return stmt;
        }, chaves);
        return Objects.requireNonNull(chaves.getKey()).longValue();
    }

    public int atualizar(long perfil, long id, BigDecimal gramas, long instante) {
        return jdbc.update("UPDATE consumo SET gramas = ?, instante = ? WHERE perfil_id = ? AND id = ?",
                gramas, instante, perfil, id);
    }

    public int excluir(long perfil, long id) {
        return jdbc.update("DELETE FROM consumo WHERE perfil_id = ? AND id = ?", perfil, id);
    }

    public Optional<Consumo.Registro> buscar(long perfil, long id, ZoneId fuso) {
        return jdbc.query("SELECT * FROM consumo WHERE perfil_id = ? AND id = ?",
                (rs, linha) -> mapear(rs, fuso), perfil, id).stream().findFirst();
    }

    public List<Consumo.Registro> listar(long perfil, LocalDate dia, ZoneId fuso) {
        return jdbc.query("""
                SELECT * FROM consumo WHERE perfil_id = ? AND instante >= ? AND instante < ?
                ORDER BY instante, id
                """, (rs, linha) -> mapear(rs, fuso), perfil,
                dia.atStartOfDay(fuso).toEpochSecond(), dia.plusDays(1).atStartOfDay(fuso).toEpochSecond());
    }

    private static Consumo.Registro mapear(ResultSet rs, ZoneId fuso) throws SQLException {
        var nutrientes = new Nutrientes(rs.getBigDecimal("calorias"), rs.getBigDecimal("carboidratos"),
                rs.getBigDecimal("proteinas"), rs.getBigDecimal("lipidios"));
        var alimento = new Alimento(rs.getString("nome"), nutrientes, rs.getString("fonte"));
        var gramas = rs.getBigDecimal("gramas");
        var horario = Instant.ofEpochSecond(rs.getLong("instante")).atZone(fuso).toOffsetDateTime();
        return new Consumo.Registro(rs.getLong("id"), alimento, gramas, horario, nutrientes.porcao(gramas));
    }
}
