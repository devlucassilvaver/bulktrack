CREATE TABLE treino (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_id BIGINT NOT NULL,
    data DATE NOT NULL,
    nome VARCHAR(120) NOT NULL,
    exercicios MEDIUMTEXT NOT NULL,
    CONSTRAINT fk_treino_perfil FOREIGN KEY (perfil_id) REFERENCES perfil(id)
);

CREATE INDEX ix_treino_data ON treino (perfil_id, data);
