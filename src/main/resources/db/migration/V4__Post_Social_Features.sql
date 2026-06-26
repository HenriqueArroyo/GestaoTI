-- ==============================================================================
-- MIGRAÇÃO V4: Funcionalidades Sociais dos Posts (Curtidas, Comentários, Favoritos)
-- ==============================================================================

-- Adiciona colunas novas na tabela existente avisos_gerais
ALTER TABLE avisos_gerais
    ADD COLUMN IF NOT EXISTS fixado        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS url_anexo     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS editado_em    TIMESTAMP NULL;

-- Tabela de Curtidas
CREATE TABLE post_curtidas (
    id           BIGSERIAL PRIMARY KEY,
    id_aviso     BIGINT NOT NULL REFERENCES avisos_gerais(id) ON DELETE CASCADE,
    id_usuario   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id_aviso, id_usuario)   -- um usuário curte uma vez por post
);

-- Tabela de Comentários
CREATE TABLE post_comentarios (
    id           BIGSERIAL PRIMARY KEY,
    id_aviso     BIGINT NOT NULL REFERENCES avisos_gerais(id) ON DELETE CASCADE,
    id_usuario   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conteudo     TEXT NOT NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    editado_em   TIMESTAMP NULL
);

-- Tabela de Favoritos
CREATE TABLE post_favoritos (
    id           BIGSERIAL PRIMARY KEY,
    id_aviso     BIGINT NOT NULL REFERENCES avisos_gerais(id) ON DELETE CASCADE,
    id_usuario   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id_aviso, id_usuario)
);

-- Índices de performance
CREATE INDEX idx_curtidas_aviso    ON post_curtidas(id_aviso);
CREATE INDEX idx_curtidas_usuario  ON post_curtidas(id_usuario);
CREATE INDEX idx_comentarios_aviso ON post_comentarios(id_aviso);
CREATE INDEX idx_favoritos_aviso   ON post_favoritos(id_aviso);
CREATE INDEX idx_favoritos_usuario ON post_favoritos(id_usuario);
CREATE INDEX idx_avisos_fixado     ON avisos_gerais(fixado);