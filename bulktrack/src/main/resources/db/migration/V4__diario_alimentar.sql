CREATE TABLE consumo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perfil_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    fonte VARCHAR(40) NOT NULL,
    calorias DECIMAL(10,4),
    carboidratos DECIMAL(10,4),
    proteinas DECIMAL(10,4),
    lipidios DECIMAL(10,4),
    gramas DECIMAL(7,2) NOT NULL,
    instante BIGINT NOT NULL,
    CONSTRAINT fk_consumo_perfil FOREIGN KEY (perfil_id) REFERENCES perfil(id)
);

CREATE INDEX ix_consumo_dia ON consumo (perfil_id, instante);
