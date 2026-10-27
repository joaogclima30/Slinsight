ALTER TABLE usuarios
    ADD COLUMN perfil VARCHAR(20) NOT NULL DEFAULT 'CIDADAO';

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_perfil
        CHECK (perfil IN ('CIDADAO', 'ADMIN'));