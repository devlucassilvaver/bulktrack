package bulktrack.entity;

import bulktrack.enums.NivelAtividade;
import bulktrack.enums.Objetivo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Perfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private LocalDate dataNascimento;
    private BigDecimal altura, pesoAtual, percentualObjetivo;

    @Enumerated(EnumType.STRING)
    private NivelAtividade nivelAtividade;

    @Enumerated(EnumType.STRING)
    private Objetivo objetivo;

    public Perfil(String nome, LocalDate dataNascimento, BigDecimal altura, BigDecimal pesoAtual, BigDecimal percentualObjetivo, NivelAtividade nivelAtividade, Objetivo objetivo) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.altura = altura;
        this.pesoAtual = pesoAtual;
        this.percentualObjetivo = percentualObjetivo;
        this.nivelAtividade = nivelAtividade;
        this.objetivo = objetivo;
    }

    public Long getId() {
        return id;
    }

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
