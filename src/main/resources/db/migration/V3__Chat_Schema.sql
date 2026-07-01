-- ==============================================================================
-- V3 - SCHEMA DO MÓDULO DE BATE-PAPO CORPORATIVO
-- ==============================================================================

CREATE TABLE canais_comunicacao (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255),
    tipo VARCHAR(20) NOT NULL
);

CREATE TABLE canal_comunicacao_usuarios (
    canal_id BIGINT NOT NULL REFERENCES canais_comunicacao(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (canal_id, user_id)
);

CREATE TABLE mensagens_comunicacao (
    id BIGSERIAL PRIMARY KEY,
    canal_id BIGINT NOT NULL REFERENCES canais_comunicacao(id) ON DELETE CASCADE,
    remetente_id BIGINT NOT NULL REFERENCES users(id),
    conteudo TEXT NOT NULL,
    enviado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensagens_comunicacao_canal ON mensagens_comunicacao(canal_id);
CREATE INDEX idx_canal_comunicacao_usuarios_user ON canal_comunicacao_usuarios(user_id);

-- Cria o canal GERAL (global) padrão, id = 1, já batendo com o mock do front (canalId: 1)
INSERT INTO canais_comunicacao (id, nome, tipo) VALUES (1, 'Chat Geral', 'GERAL');

-- Corrige a sequence pra não colidir com o id fixo acima
SELECT setval(pg_get_serial_sequence('canais_comunicacao', 'id'), (SELECT MAX(id) FROM canais_comunicacao));