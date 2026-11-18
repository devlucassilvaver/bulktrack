package bulktrack.service;

import bulktrack.dto.*;
import bulktrack.exception.PerfilNaoEncontradoException;
import bulktrack.repository.ConsumoRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DiarioService {
    private final PerfilService perfis;
    private final ConsumoRepository consumos;

    public record Balanco(BigDecimal meta, BigDecimal total, BigDecimal restante, BigDecimal excedente,
                          int registrosSemDado) {}
    public record Resumo(LocalDate dia, Balanco calorias, Balanco carboidratos, Balanco proteinas,
                         Balanco lipidios, List<Consumo.Registro> registros) {}

    public DiarioService(PerfilService perfis, ConsumoRepository consumos) {
        this.perfis = perfis;
        this.consumos = consumos;
    }

    public Consumo.Registro registrar(String usuario, long perfilId, Consumo.Novo pedido) {
        var perfil = perfis.proprio(usuario, perfilId);
        var fuso = ZoneId.of(perfil.getFuso());
        var entrada = pedido.alimento();
        var alimento = new Alimento(entrada.nome().strip(), entrada.por100g(),
                entrada.fonte() == null || entrada.fonte().isBlank() ? "Manual" : entrada.fonte());
        long id = consumos.inserir(perfilId, alimento, pedido.consumo().gramas(), instante(pedido.consumo(), fuso));
        return buscar(perfilId, id, fuso);
    }

    public Consumo.Registro atualizar(String usuario, long perfilId, long id, Consumo.Quantidade quantidade) {
        var perfil = perfis.proprio(usuario, perfilId);
        var fuso = ZoneId.of(perfil.getFuso());
        consumos.atualizar(perfilId, id, quantidade.gramas(), instante(quantidade, fuso));
        return buscar(perfilId, id, fuso);
    }

    public void excluir(String usuario, long perfilId, long id) {
        perfis.proprio(usuario, perfilId);
        if (consumos.excluir(perfilId, id) == 0) {
            throw ausente();
        }
    }

    @Transactional(readOnly = true)
    public Resumo resumo(String usuario, long perfilId, LocalDate dia) {
        var perfil = perfis.proprio(usuario, perfilId);
        if (dia == null) {
            dia = perfis.hoje(perfil);
        }
        validarDia(dia);
        var registros = consumos.listar(perfilId, dia, ZoneId.of(perfil.getFuso()));
        var meta = perfis.metaEm(perfil, dia);
        return new Resumo(dia,
                balanco(registros, Nutrientes::calorias, meta == null ? null : meta.calorias()),
                balanco(registros, Nutrientes::carboidratos, meta == null ? null : meta.carboidratos()),
                balanco(registros, Nutrientes::proteinas, meta == null ? null : meta.proteinas()),
                balanco(registros, Nutrientes::lipidios, meta == null ? null : meta.lipidios()), registros);
    }

    private static Balanco balanco(List<Consumo.Registro> registros,
                                   Function<Nutrientes, BigDecimal> nutriente, BigDecimal meta) {
        var conhecidos = registros.stream().map(registro -> nutriente.apply(registro.nutrientes()))
                .filter(Objects::nonNull).toList();
        int faltantes = registros.size() - conhecidos.size();
        BigDecimal total = conhecidos.isEmpty() && faltantes > 0 ? null
                : conhecidos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferenca = meta == null || faltantes > 0 ? null : meta.subtract(total);
        return new Balanco(meta, total, diferenca == null ? null : diferenca.max(BigDecimal.ZERO),
                diferenca == null ? null : diferenca.negate().max(BigDecimal.ZERO), faltantes);
    }

    private Consumo.Registro buscar(long perfilId, long id, ZoneId fuso) {
        return consumos.buscar(perfilId, id, fuso).orElseThrow(DiarioService::ausente);
    }

    private static PerfilNaoEncontradoException ausente() {
        return new PerfilNaoEncontradoException("Consumo não encontrado.");
    }

    private static long instante(Consumo.Quantidade quantidade, ZoneId fuso) {
        validarDia(quantidade.horario().toLocalDate());
        var offsets = fuso.getRules().getValidOffsets(quantidade.horario());
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException("Este horário não existe no fuso escolhido.");
        }
        return quantidade.horario().toEpochSecond(offsets.getFirst());
    }

    private static void validarDia(LocalDate dia) {
        if (dia.getYear() < 1900 || dia.getYear() > 9998) {
            throw new IllegalArgumentException("Informe um ano entre 1900 e 9998.");
        }
    }
}
