-- ==============================================================================
-- GESTÃO T.I. 2.0 - SCRIPT DE CRIAÇÃO DE BANCO DE DADOS (POSTGRESQL)
-- ==============================================================================

-- 1. Tabela de Departamentos (Mantida para normalização de usuários)
CREATE TABLE departamentos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

-- 2. Tabela Unificada de Usuários (Consolida 'usuarios' e 'usuarios_sistema')
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- Ex: ADMIN, TECNICO, USER
    id_departamento INT REFERENCES departamentos(id) ON DELETE SET NULL,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE
);

-- 3. Tabela Unificada de Ativos/CMDB (Consolida computadores, impressoras, cameras, ramais, pabx)
CREATE TABLE ativos (
    id BIGSERIAL PRIMARY KEY,
    tipo_ativo VARCHAR(50) NOT NULL, -- Ex: COMPUTADOR, IMPRESSORA, CAMERA, RAMAL, PABX
    nome_equipamento VARCHAR(255) NOT NULL,
    ip VARCHAR(50),
    local_setor VARCHAR(100),
    id_usuario_responsavel INT REFERENCES users(id) ON DELETE SET NULL,
    detalhes_tecnicos JSONB, -- Armazena dados específicos flexíveis (ex: {"ram": "16GB", "processador": "i7"} para PCs, ou {"troncos": "4"} para PABX)
    observacao TEXT,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela Unificada de Credenciais (Consolida administracao, email, vpn, wifi, rm, etc)
CREATE TABLE credenciais (
    id BIGSERIAL PRIMARY KEY,
    tipo_credencial VARCHAR(50) NOT NULL, -- Ex: EMAIL, VPN, WIFI, SISTEMA_ERP, BANCO_DE_DADOS
    identificador VARCHAR(255) NOT NULL, -- Ex: o endereço de email, o SSID do wifi, o link do sistema
    usuario VARCHAR(255),
    senha VARCHAR(255) NOT NULL, -- Deve ser salva criptografada pela aplicação Java
    observacao TEXT,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabela Unificada de Vencimentos (Consolida certificados, contratos, dominios, licencas)
CREATE TABLE contratos_licencas (
    id BIGSERIAL PRIMARY KEY,
    tipo_item VARCHAR(50) NOT NULL, -- Ex: CONTRATO, LICENCA, DOMINIO, CERTIFICADO
    descricao VARCHAR(255) NOT NULL,
    chave_acesso TEXT, -- Usado para seriais de licenças
    link_anexo TEXT,
    data_vencimento DATE NOT NULL,
    precisa_renovar BOOLEAN DEFAULT TRUE,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tabela de Chamados (Help Desk modernizado)
CREATE TABLE chamados (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    categoria VARCHAR(100),
    empresa VARCHAR(100), -- Ex: Engebag, Bag Cleaner
    criticidade VARCHAR(20) NOT NULL DEFAULT 'BAIXA', -- BAIXA, MEDIA, ALTA, CRITICA
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO', -- ABERTO, EM_ANDAMENTO, FECHADO
    id_usuario_abriu INT NOT NULL REFERENCES users(id),
    id_tecnico_atribuido INT REFERENCES users(id) ON DELETE SET NULL,
    data_abertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_fechamento TIMESTAMP NULL,
    sla_cumprido BOOLEAN NULL -- Será preenchido automaticamente ao fechar o chamado
);

-- 7. Tabela de Mensagens do Chat (Vinculada aos Chamados)
CREATE TABLE mensagens_chamado (
    id BIGSERIAL PRIMARY KEY,
    id_chamado INT NOT NULL REFERENCES chamados(id) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES users(id),
    mensagem TEXT NOT NULL,
    tipo_mensagem VARCHAR(20) NOT NULL DEFAULT 'TEXTO', -- TEXTO, ARQUIVO
    url_arquivo VARCHAR(255),
    nome_original_arquivo VARCHAR(255),
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Tabela de Monitoramento de Backups (Baseado na aba nova solicitada)
CREATE TABLE logs_backup (
    id BIGSERIAL PRIMARY KEY,
    nome_rotina VARCHAR(255) NOT NULL, -- Ex: 'Backup ERP RM', 'Backup File Server'
    status VARCHAR(20) NOT NULL, -- SUCESSO, FALHA
    detalhes TEXT,
    data_execucao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- CRIAÇÃO DE ÍNDICES PARA OTIMIZAÇÃO DE BUSCAS
-- ==============================================================================

-- Índices para buscas frequentes no Dashboard e Telas de Listagem
CREATE INDEX idx_chamados_status ON chamados(status);
CREATE INDEX idx_chamados_tecnico ON chamados(id_tecnico_atribuido);
CREATE INDEX idx_vencimentos_data ON contratos_licencas(data_vencimento);
CREATE INDEX idx_ativos_tipo ON ativos(tipo_ativo);
CREATE INDEX idx_credenciais_tipo ON credenciais(tipo_credencial);
CREATE INDEX idx_mensagens_chamado ON mensagens_chamado(id_chamado);