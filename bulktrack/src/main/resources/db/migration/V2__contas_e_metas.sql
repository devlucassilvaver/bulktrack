CREATE TABLE conta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(64) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL
);

ALTER TABLE perfil ADD COLUMN conta_id BIGINT;
ALTER TABLE perfil ADD CONSTRAINT uq_perfil_conta UNIQUE (conta_id);
ALTER TABLE perfil ADD COLUMN parametro_tmb VARCHAR(16);
ALTER TABLE perfil ADD COLUMN fuso VARCHAR(64) NOT NULL DEFAULT 'America/Fortaleza';
ALTER TABLE perfil ADD CONSTRAINT fk_perfil_conta FOREIGN KEY (conta_id) REFERENCES conta(id);

CREATE TABLE meta_vigente (
    perfil_id BIGINT NOT NULL,
    inicio DATE NOT NULL,
    calorias DECIMAL(10,0) NOT NULL,
    carboidratos DECIMAL(10,1) NOT NULL,
    proteinas DECIMAL(10,1) NOT NULL,
    lipidios DECIMAL(10,1) NOT NULL,
    PRIMARY KEY (perfil_id, inicio),
    CONSTRAINT fk_meta_perfil FOREIGN KEY (perfil_id) REFERENCES perfil(id)
);
