package bulktrack.service;

import bulktrack.dto.*;
import bulktrack.entity.*;
import bulktrack.exception.PerfilNaoEncontradoException;
import bulktrack.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PerfilService {
    private final PerfilRepository perfis;
    private final HistoricoPesoRepository pesos;
    private final Contas contas;
    private final JdbcTemplate jdbc;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entidades;

    public record Pesagem(Long id, LocalDate data, BigDecimal peso) {}

    public PerfilService(PerfilRepository perfis, HistoricoPesoRepository pesos, Contas contas, JdbcTemplate jdbc) {
        this.perfis = perfis;
        this.pesos = pesos;
        this.contas = contas;
        this.jdbc = jdbc;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PerfilResponse criarPerfil(String usuario, PerfilRequest pedido) {
        long conta = contas.id(usuario);
        jdbc.queryForObject("SELECT id FROM conta WHERE id = ? FOR UPDATE", Long.class, conta);
        if (perfis.findByContaId(conta).isPresent()) {
            throw new IllegalArgumentException("Esta conta já tem perfil.");
        }
        Perfil perfil = new Perfil();
        perfil.setContaId(conta);
        return salvar(perfil, pedido);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PerfilResponse atualizar(String usuario, long id, PerfilRequest pedido) {
        Perfil perfil = proprio(usuario, id);
        jdbc.queryForObject("SELECT id FROM perfil WHERE id = ? FOR UPDATE", Long.class, id);
        entidades.refresh(perfil);
        return salvar(perfil, pedido);
    }

    private PerfilResponse salvar(Perfil perfil, PerfilRequest pedido) {
        String fuso = pedido.fuso() == null || pedido.fuso().isBlank() ? perfil.getFuso() : pedido.fuso();
        if (!ZoneId.getAvailableZoneIds().contains(fuso)) {
            throw new IllegalArgumentException("Informe um fuso IANA, como America/Fortaleza.");
        }
        LocalDate hoje = LocalDate.now(ZoneId.of(fuso));
        if (pedido.dataNascimento().isAfter(hoje.minusYears(18))
                || pedido.dataNascimento().isBefore(hoje.minusYears(130))) {
            throw new IllegalArgumentException("Informe uma data de nascimento entre 18 e 130 anos atrás.");
        }
        var personalizada = pedido.metaPersonalizada();
        if (personalizada != null && personalizada.energiaDosMacros().subtract(personalizada.calorias())
                .abs().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Os macros devem corresponder às calorias com tolerância de 1 kcal.");
        }
        boolean pesagem = perfil.getPesoAtual() == null || pedido.pesoAtual().compareTo(perfil.getPesoAtual()) != 0;
        perfil.setNome(pedido.nome().strip());
        perfil.setDataNascimento(pedido.dataNascimento());
        perfil.setSexo(pedido.sexo());
        perfil.setParametroTmb(pedido.parametroTmb());
        perfil.setAltura(pedido.altura());
        perfil.setPesoAtual(pedido.pesoAtual());
        perfil.setNivelAtividade(pedido.nivelAtividade());
        perfil.setObjetivo(pedido.objetivo());
        perfil.setPercentualObjetivo(pedido.percentualObjetivo());
        perfil.setFuso(fuso);
        perfis.saveAndFlush(perfil);
        if (pesagem) {
            salvarPeso(perfil, pedido.pesoAtual(), hoje);
        }
        salvarPersonalizada(perfil.getId(), personalizada);
        gravarMeta(perfil, personalizada);
        return resposta(perfil);
    }

    public Perfil proprio(String usuario, Long id) {
        long conta = contas.id(usuario);
        return perfis.findByContaId(conta).filter(perfil -> id == null || id.equals(perfil.getId()))
                .orElseThrow(() -> new PerfilNaoEncontradoException("Perfil não encontrado."));
    }

    @Transactional(readOnly = true)
    public PerfilResponse buscar(String usuario, Long id) {
        return resposta(proprio(usuario, id));
    }

    private PerfilResponse resposta(Perfil perfil) {
        var hoje = hoje(perfil);
        var estimativa = perfil.getParametroTmb() == null ? null : CalculadoraNutricional.calcular(perfil, hoje);
        var ajuste = personalizada(perfil.getId());
        return new PerfilResponse(perfil.getId(), perfil.getNome(), perfil.getDataNascimento(), perfil.getAltura(),
                perfil.getPesoAtual(), perfil.getNivelAtividade(), perfil.getObjetivo(), perfil.getPercentualObjetivo(),
                perfil.getSexo(), perfil.getParametroTmb(), perfil.getFuso(), hoje,
                estimativa == null ? null : estimativa.tmb(), estimativa == null ? null : estimativa.get(),
                metaEm(perfil, hoje), ajuste);
    }

    public LocalDate hoje(Perfil perfil) {
        return LocalDate.now(ZoneId.of(perfil.getFuso()));
    }

    @Transactional(readOnly = true)
    public List<Pesagem> historico(String usuario, long id) {
        Perfil perfil = proprio(usuario, id);
        return pesos.findByPerfilOrderByDataDescIdDesc(perfil).stream()
                .map(p -> new Pesagem(p.getId(), p.getData(), p.getPeso())).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void registrarPeso(String usuario, long id, HistoricoPesoRequest pedido) {
        Perfil perfil = proprio(usuario, id);
        jdbc.queryForObject("SELECT id FROM perfil WHERE id = ? FOR UPDATE", Long.class, id);
        entidades.refresh(perfil);
        LocalDate hoje = hoje(perfil);
        if (pedido.getData().isAfter(hoje) || pedido.getPeso().compareTo(BigDecimal.valueOf(700)) > 0
                || pedido.getPeso().scale() > 2 || pedido.getPeso().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Informe uma data até hoje e peso de 1 a 700 kg, com até duas casas.");
        }
        var anterior = pesos.findFirstByPerfilOrderByDataDescIdDesc(perfil);
        salvarPeso(perfil, pedido.getPeso(), pedido.getData());
        if (anterior.isEmpty() || !pedido.getData().isBefore(anterior.get().getData())) {
            perfil.setPesoAtual(pedido.getPeso());
            gravarMeta(perfil, personalizada(id));
        }
    }

    private void salvarPeso(Perfil perfil, BigDecimal peso, LocalDate data) {
        var registro = new HistoricoPeso();
        registro.setPerfil(perfil);
        registro.setPeso(peso);
        registro.setData(data);
        pesos.save(registro);
    }

    private void gravarMeta(Perfil perfil, MetaNutricional personalizada) {
        if (perfil.getParametroTmb() == null) {
            return;
        }
        LocalDate hoje = hoje(perfil);
        var meta = personalizada == null ? CalculadoraNutricional.calcular(perfil, hoje).meta() : personalizada;
        jdbc.update("DELETE FROM meta_vigente WHERE perfil_id = ? AND inicio = ?", perfil.getId(), hoje);
        jdbc.update("INSERT INTO meta_vigente VALUES (?, ?, ?, ?, ?, ?)", perfil.getId(), hoje,
                meta.calorias(), meta.carboidratos(), meta.proteinas(), meta.lipidios());
    }

    private void salvarPersonalizada(long id, MetaNutricional meta) {
        jdbc.update("DELETE FROM meta_personalizada WHERE perfil_id = ?", id);
        if (meta != null) {
            jdbc.update("INSERT INTO meta_personalizada VALUES (?, ?, ?, ?, ?)", id, meta.calorias(),
                    meta.carboidratos(), meta.proteinas(), meta.lipidios());
        }
    }

    private MetaNutricional personalizada(long id) {
        return consultarMeta("SELECT calorias, carboidratos, proteinas, lipidios FROM meta_personalizada WHERE perfil_id = ?", id);
    }

    public MetaNutricional metaEm(Perfil perfil, LocalDate data) {
        return consultarMeta("""
                SELECT calorias, carboidratos, proteinas, lipidios FROM meta_vigente
                WHERE perfil_id = ? AND inicio <= ? ORDER BY inicio DESC LIMIT 1
                """, perfil.getId(), data);
    }

    private MetaNutricional consultarMeta(String sql, Object... argumentos) {
        return jdbc.query(sql, (rs, linha) -> new MetaNutricional(rs.getBigDecimal(1), rs.getBigDecimal(2),
                rs.getBigDecimal(3), rs.getBigDecimal(4)), argumentos).stream().findFirst().orElse(null);
    }
}
