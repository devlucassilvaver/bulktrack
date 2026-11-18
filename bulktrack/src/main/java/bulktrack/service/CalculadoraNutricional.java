package bulktrack.service;

import bulktrack.dto.MetaNutricional;
import bulktrack.entity.Perfil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

public final class CalculadoraNutricional {
    private CalculadoraNutricional() {}

    public record Estimativa(BigDecimal tmb, BigDecimal get, MetaNutricional meta) {}

    public static Estimativa calcular(Perfil perfil, LocalDate dia) {
        int idade = Period.between(perfil.getDataNascimento(), dia).getYears();
        int constante = switch (perfil.getParametroTmb()) {
            case MAIS_5 -> 5;
            case MENOS_161 -> -161;
        };
        BigDecimal tmb = perfil.getPesoAtual().multiply(decimal(10))
                .add(perfil.getAltura().multiply(decimal(625)))
                .add(decimal(constante - 5L * idade)).max(BigDecimal.ONE);
        BigDecimal fator = switch (perfil.getNivelAtividade()) {
            case SEDENTARIO -> decimal(1.2);
            case LEVE -> decimal(1.38);
            case MODERADO -> decimal(1.55);
            case ALTO -> decimal(1.73);
            case MUITO_ALTO -> decimal(1.9);
        };
        BigDecimal get = tmb.multiply(fator);
        BigDecimal percentual = perfil.getPercentualObjetivo();
        BigDecimal ajuste = switch (perfil.getObjetivo()) {
            case MANUTENCAO -> BigDecimal.ZERO;
            case GANHO_MUSCULAR -> percentual == null ? decimal(10) : percentual;
            case PERDA_GORDURA -> (percentual == null ? decimal(15) : percentual).negate();
        };
        BigDecimal calorias = get.multiply(BigDecimal.ONE.add(ajuste.movePointLeft(2)))
                .setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ONE);
        var meta = new MetaNutricional(calorias, gramas(calorias, 0.50, 4),
                gramas(calorias, 0.25, 4), gramas(calorias, 0.25, 9));
        return new Estimativa(tmb.setScale(2, RoundingMode.HALF_UP),
                get.setScale(2, RoundingMode.HALF_UP), meta);
    }

    private static BigDecimal gramas(BigDecimal calorias, double proporcao, int kcalPorGrama) {
        return calorias.multiply(decimal(proporcao))
                .divide(decimal(kcalPorGrama), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(double numero) {
        return BigDecimal.valueOf(numero);
    }
}
