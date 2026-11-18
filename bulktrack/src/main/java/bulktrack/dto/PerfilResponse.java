package bulktrack.dto;

import bulktrack.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PerfilResponse(Long id, String nome, LocalDate dataNascimento, BigDecimal altura,
        BigDecimal pesoAtual, NivelAtividade nivelAtividade, Objetivo objetivo,
        BigDecimal percentualObjetivo, Sexo sexo, ParametroTmb parametroTmb, String fuso,
        LocalDate hoje, BigDecimal tmb, BigDecimal get, MetaNutricional meta,
        MetaNutricional metaPersonalizada) {}
