CREATE TABLE meta_personalizada (
    perfil_id BIGINT PRIMARY KEY,
    calorias DECIMAL(10,0) NOT NULL,
    carboidratos DECIMAL(10,1) NOT NULL,
    proteinas DECIMAL(10,1) NOT NULL,
    lipidios DECIMAL(10,1) NOT NULL,
    CONSTRAINT fk_meta_personalizada_perfil FOREIGN KEY (perfil_id) REFERENCES perfil(id)
);
