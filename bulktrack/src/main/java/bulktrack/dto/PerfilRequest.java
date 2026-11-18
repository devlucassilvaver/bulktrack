package bulktrack.dto;

import bulktrack.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PerfilRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotNull LocalDate dataNascimento,
        @NotNull @DecimalMin("0.50") @DecimalMax("2.80") @Digits(integer = 1, fraction = 2) BigDecimal altura,
        @NotNull @DecimalMin("1.00") @DecimalMax("700") @Digits(integer = 3, fraction = 2) BigDecimal pesoAtual,
        @NotNull NivelAtividade nivelAtividade,
        @NotNull Objetivo objetivo,
        @DecimalMin("0") @DecimalMax("50") @Digits(integer = 2, fraction = 2) BigDecimal percentualObjetivo,
        Sexo sexo,
        @NotNull ParametroTmb parametroTmb,
        @Size(max = 64) String fuso,
        @Valid MetaNutricional metaPersonalizada) {}
