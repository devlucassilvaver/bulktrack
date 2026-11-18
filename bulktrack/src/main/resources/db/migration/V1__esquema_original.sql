CREATE TABLE IF NOT EXISTS perfil (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255),
    data_nascimento DATE,
    altura DECIMAL(38,2),
    peso_atual DECIMAL(38,2),
    percentual_objetivo DECIMAL(38,2),
    nivel_atividade VARCHAR(30),
    objetivo VARCHAR(30),
    sexo VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS historico_peso (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    peso DECIMAL(38,2),
    data DATE,
    perfil_id BIGINT,
    CONSTRAINT fk_pesagem_perfil FOREIGN KEY (perfil_id) REFERENCES perfil(id)
);
