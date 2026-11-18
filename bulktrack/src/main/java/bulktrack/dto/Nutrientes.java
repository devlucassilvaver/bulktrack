package bulktrack.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Nutrientes(
        @PositiveOrZero @Digits(integer = 4, fraction = 4) BigDecimal calorias,
        @PositiveOrZero @Digits(integer = 4, fraction = 4) BigDecimal carboidratos,
        @PositiveOrZero @Digits(integer = 4, fraction = 4) BigDecimal proteinas,
        @PositiveOrZero @Digits(integer = 4, fraction = 4) BigDecimal lipidios) {
    public Nutrientes porcao(BigDecimal gramas) {
        return new Nutrientes(escalar(calorias, gramas), escalar(carboidratos, gramas),
                escalar(proteinas, gramas), escalar(lipidios, gramas));
    }

    private static BigDecimal escalar(BigDecimal valor, BigDecimal gramas) {
        return valor == null ? null : valor.multiply(gramas).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }
}
