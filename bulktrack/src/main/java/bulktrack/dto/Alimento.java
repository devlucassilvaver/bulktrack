package bulktrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record Alimento(@NotBlank @Size(max = 255) String nome, @Valid @NotNull Nutrientes por100g,
        @Size(max = 40) String fonte) {}
