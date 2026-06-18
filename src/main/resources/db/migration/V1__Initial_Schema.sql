-- ==============================================================================
-- GESTÃO T.I. 2.0 - SCRIPT DE CRIAÇÃO DE BANCO DE DADOS (POSTGRESQL)
-- ==============================================================================

-- 1. Tabela de Departamentos
CREATE TABLE departamentos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

-- 2. Tabela Unificada de Usuários
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- ADMIN, TECNICO, USER
    empresa_acesso VARCHAR(50) NOT NULL DEFAULT 'AMBAS', -- ENGEBAG, BAG_CLEANER, AMBAS
    id_departamento INT REFERENCES departamentos(id) ON DELETE SET NULL,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE
);

-- 3. Tabela Unificada de Ativos/CMDB
CREATE TABLE ativos (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL, -- ENGEBAG, BAG_CLEANER
    tipo_ativo VARCHAR(50) NOT NULL, 
    nome_equipamento VARCHAR(255) NOT NULL,
    ip VARCHAR(50),
    local_setor VARCHAR(100),
    id_usuario_responsavel INT REFERENCES users(id) ON DELETE SET NULL,
    detalhes_tecnicos JSONB, 
    observacao TEXT,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela Unificada de Credenciais
CREATE TABLE credenciais (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL, -- ENGEBAG, BAG_CLEANER, AMBAS
    tipo_credencial VARCHAR(50) NOT NULL, 
    identificador VARCHAR(255) NOT NULL, 
    usuario VARCHAR(255),
    senha VARCHAR(255) NOT NULL, 
    observacao TEXT,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabela Unificada de Vencimentos
CREATE TABLE contratos_licencas (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL, -- ENGEBAG, BAG_CLEANER, AMBAS
    tipo_item VARCHAR(50) NOT NULL, 
    descricao VARCHAR(255) NOT NULL,
    chave_acesso TEXT, 
    link_anexo TEXT,
    data_vencimento DATE NOT NULL,
    precisa_renovar BOOLEAN DEFAULT TRUE,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tabela de Chamados
CREATE TABLE chamados (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    categoria VARCHAR(100),
    empresa VARCHAR(50) NOT NULL, -- ENGEBAG, BAG_CLEANER
    criticidade VARCHAR(20) NOT NULL DEFAULT 'BAIXA', 
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO', 
    id_usuario_abriu INT NOT NULL REFERENCES users(id),
    id_tecnico_atribuido INT REFERENCES users(id) ON DELETE SET NULL,
    data_abertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_fechamento TIMESTAMP NULL,
    sla_cumprido BOOLEAN NULL 
);

-- 7. Tabela de Mensagens do Chat
CREATE TABLE mensagens_chamado (
    id BIGSERIAL PRIMARY KEY,
    id_chamado INT NOT NULL REFERENCES chamados(id) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES users(id),
    mensagem TEXT NOT NULL,
    tipo_mensagem VARCHAR(20) NOT NULL DEFAULT 'TEXTO', 
    url_arquivo VARCHAR(255),
    nome_original_arquivo VARCHAR(255),
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Tabela de Monitoramento de Backups
CREATE TABLE logs_backup (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL, -- ENGEBAG, BAG_CLEANER
    nome_rotina VARCHAR(255) NOT NULL, 
    status VARCHAR(20) NOT NULL, 
    detalhes TEXT,
    data_execucao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Tabela de Lembretes da Equipe de T.I.
CREATE TABLE lembretes (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    data_alerta TIMESTAMP, -- Data em que o sistema deve notificar
    frequencia VARCHAR(20), -- Única, Semanal, Mensal, Anual
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, CONCLUIDO
    id_usuario_criador INT REFERENCES users(id) ON DELETE SET NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- CRIAÇÃO DE ÍNDICES PARA OTIMIZAÇÃO DE BUSCAS
-- ==============================================================================
CREATE INDEX idx_chamados_status ON chamados(status);
CREATE INDEX idx_chamados_tecnico ON chamados(id_tecnico_atribuido);
CREATE INDEX idx_vencimentos_data ON contratos_licencas(data_vencimento);
CREATE INDEX idx_ativos_tipo ON ativos(tipo_ativo);
CREATE INDEX idx_credenciais_tipo ON credenciais(tipo_credencial);
CREATE INDEX idx_mensagens_chamado ON mensagens_chamado(id_chamado);
CREATE INDEX idx_lembretes_status ON lembretes(status);