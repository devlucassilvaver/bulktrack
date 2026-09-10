package bulktrack.dto;

import bulktrack.enums.NivelAtividade;
import bulktrack.enums.Objetivo;
import bulktrack.enums.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PerfilResponse {
    private Long id;
    private String nome;
    private LocalDate dataNascimento;
    private BigDecimal altura;
    private BigDecimal pesoAtual;
    private NivelAtividade nivelAtividade;
    private Objetivo objetivo;
    private BigDecimal percentualObjetivo;
    private Sexo sexo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getPercentualObjetivo() {
        return percentualObjetivo;
    }

    public void setPercentualObjetivo(BigDecimal percentualObjetivo) {
        this.percentualObjetivo = percentualObjetivo;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }
}
