package bulktrack;

import bulktrack.dto.*;
import bulktrack.enums.*;
import bulktrack.exception.PerfilNaoEncontradoException;
import bulktrack.service.PerfilService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PerfilMvpTests {
    @Autowired PerfilService perfis;
    @Autowired JdbcTemplate jdbc;

    @Test
    void calculaMetasEPreservaDiasAnteriores() {
        criarConta("ana");
        criarConta("bia");
        LocalDate hoje = LocalDate.now(java.time.ZoneId.of("America/Fortaleza"));
        var perfil = perfis.criarPerfil("ana", new PerfilRequest("Ana", hoje.minusYears(30),
                new BigDecimal("1.80"), new BigDecimal("80"), NivelAtividade.MODERADO,
                Objetivo.GANHO_MUSCULAR, null, null, ParametroTmb.MAIS_5, null, null));
        assertEquals(new BigDecimal("1780.00"), perfil.tmb());
        assertEquals(new BigDecimal("2759.00"), perfil.get());
        assertEquals(new BigDecimal("3035"), perfil.meta().calorias());
        assertTrue(perfil.meta().energiaDosMacros().subtract(perfil.meta().calorias()).abs()
                .compareTo(BigDecimal.ONE) <= 0);
        jdbc.update("UPDATE meta_vigente SET inicio = ? WHERE perfil_id = ?", hoje.minusDays(1), perfil.id());
        var pesagem = new HistoricoPesoRequest();
        pesagem.setData(hoje);
        pesagem.setPeso(new BigDecimal("90"));
        perfis.registrarPeso("ana", perfil.id(), pesagem);
        assertEquals(2, perfis.historico("ana", perfil.id()).size());
        var entidade = perfis.proprio("ana", perfil.id());
        assertEquals(new BigDecimal("3035"), perfis.metaEm(entidade, hoje.minusDays(1)).calorias());
        assertEquals(new BigDecimal("3205"), perfis.buscar("ana", perfil.id()).meta().calorias());
        assertThrows(PerfilNaoEncontradoException.class, () -> perfis.buscar("bia", perfil.id()));
        assertThrows(PerfilNaoEncontradoException.class, () -> perfis.registrarPeso("bia", perfil.id(), pesagem));
    }

    private void criarConta(String usuario) {
        jdbc.update("INSERT INTO conta(usuario, senha_hash) VALUES (?, ?)", usuario, "hash-de-teste");
    }
}
