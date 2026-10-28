ALTER TABLE localizacoes
    ADD COLUMN autor_id BIGINT REFERENCES usuarios(id);

CREATE INDEX idx_localizacoes_autor
    ON localizacoes(autor_id);