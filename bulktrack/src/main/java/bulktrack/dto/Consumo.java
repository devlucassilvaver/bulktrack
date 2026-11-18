package bulktrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;

public final class Consumo {
    private Consumo() {}

    public record Novo(@NotNull @Valid Alimento alimento,
                       @NotNull @Valid Quantidade consumo) {}

    public record Quantidade(@NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal gramas,
                             @NotNull LocalDateTime horario) {}

    public record Registro(long id, Alimento alimento, BigDecimal gramas,
                           OffsetDateTime horario, Nutrientes nutrientes) {}
}
