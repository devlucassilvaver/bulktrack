package bulktrack.dto;

import bulktrack.enums.NivelAtividade;
import bulktrack.enums.Objetivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PerfilRequest {

    @NotBlank
    private String nome;

    @NotNull
    private LocalDate dataNascimento;

    @NotNull
    @Positive
    private BigDecimal altura;

    @NotNull
    @Positive
    private BigDecimal pesoAtual;

    @NotNull
    private NivelAtividade nivelAtividade;

    @NotNull
    private Objetivo objetivo;

    @NotNull
    @Positive
    private BigDecimal percentualObjetivo;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public BigDecimal getAltura() {
        return altura;
    }

    public void setAltura(BigDecimal altura) {
        this.altura = altura;
    }

    public BigDecimal getPesoAtual() {
        return pesoAtual;
    }

    public void setPesoAtual(BigDecimal pesoAtual) {
        this.pesoAtual = pesoAtual;
    }

    public BigDecimal getPercentualObjetivo() {
        return percentualObjetivo;
    }

    public void setPercentualObjetivo(BigDecimal percentualObjetivo) {
        this.percentualObjetivo = percentualObjetivo;
    }

    public NivelAtividade getNivelAtividade() {
        return nivelAtividade;
    }

    public void setNivelAtividade(NivelAtividade nivelAtividade) {
        this.nivelAtividade = nivelAtividade;
    }

    public Objetivo getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(Objetivo objetivo) {
        this.objetivo = objetivo;
    }
}
