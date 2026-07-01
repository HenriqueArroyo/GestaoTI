-- Adiciona suporte a respostas em comentários (threading de 1 nível)
ALTER TABLE post_comentarios
    ADD COLUMN IF NOT EXISTS id_pai BIGINT REFERENCES post_comentarios(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_comentarios_pai ON post_comentarios(id_pai);