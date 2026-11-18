package bulktrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Treino {
    private Treino() {}

    public record Serie(@NotNull @Min(1) @Max(1000) Integer repeticoes,
                        @PositiveOrZero @Digits(integer = 5, fraction = 2) BigDecimal cargaKg) {}

    public record Exercicio(@NotBlank @Size(max = 120) String nome,
                            @NotNull @Min(0) @Max(3600) Integer descansoSegundos,
                            @NotEmpty @Size(max = 50) List<@NotNull @Valid Serie> series) {}

    public record Sessao(@NotNull LocalDate data, @NotBlank @Size(max = 120) String nome,
                         @NotEmpty @Size(max = 50) List<@NotNull @Valid Exercicio> exercicios) {}

    public record Registro(long id, LocalDate data, String nome, List<Exercicio> exercicios) {}
}
