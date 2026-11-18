package bulktrack.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MetaNutricional(
        @NotNull @Positive @Digits(integer = 5, fraction = 0) BigDecimal calorias,
        @NotNull @PositiveOrZero @Digits(integer = 5, fraction = 1) BigDecimal carboidratos,
        @NotNull @PositiveOrZero @Digits(integer = 5, fraction = 1) BigDecimal proteinas,
        @NotNull @PositiveOrZero @Digits(integer = 5, fraction = 1) BigDecimal lipidios) {

    public BigDecimal energiaDosMacros() {
        return carboidratos.add(proteinas).multiply(BigDecimal.valueOf(4))
                .add(lipidios.multiply(BigDecimal.valueOf(9)));
    }
}
