-- ==============================================================================
-- GESTÃO T.I. 3.2 - SCRIPT DE CRIAÇÃO DE BANCO DE DADOS (POSTGRESQL)
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
    cargo VARCHAR(100), 
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    empresa_acesso VARCHAR(50) NOT NULL DEFAULT 'AMBAS',
    id_departamento BIGINT REFERENCES departamentos(id) ON DELETE SET NULL,
    usuario_rm VARCHAR(100), 
    utiliza_omaxprensa BOOLEAN DEFAULT FALSE,
    foto_perfil VARCHAR(255), 
    primeiro_acesso BOOLEAN DEFAULT TRUE, 
    ultimo_login TIMESTAMP, 
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE
);

-- 3. Tabela Unificada de Ativos/CMDB
CREATE TABLE ativos (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL,
    tipo_ativo VARCHAR(50) NOT NULL, 
    nome_equipamento VARCHAR(255) NOT NULL,
    url_imagem VARCHAR(255), 
    ip VARCHAR(50),
    local_setor VARCHAR(100),
    id_usuario_responsavel BIGINT REFERENCES users(id) ON DELETE SET NULL,
    detalhes_tecnicos JSONB, 
    observacao TEXT,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela Unificada de Credenciais
CREATE TABLE credenciais (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL,
    id_usuario_dono BIGINT REFERENCES users(id) ON DELETE CASCADE, 
    tipo_credencial VARCHAR(50) NOT NULL, 
    identificador VARCHAR(255) NOT NULL, 
    usuario VARCHAR(255),
    senha VARCHAR(255), -- ALTERADO: Agora permite ser nulo
    observacao TEXT,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabela Unificada de Vencimentos
CREATE TABLE contratos_licencas (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL,
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
    empresa VARCHAR(50) NOT NULL,
    criticidade VARCHAR(20) NOT NULL DEFAULT 'BAIXA', 
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO', 
    id_usuario_abriu BIGINT NOT NULL REFERENCES users(id),
    id_tecnico_atribuido BIGINT REFERENCES users(id) ON DELETE SET NULL,
    data_abertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_fechamento TIMESTAMP NULL,
    sla_cumprido BOOLEAN NULL 
);

-- 7. Tabela de Mensagens do Chat
CREATE TABLE mensagens_chamado (
    id BIGSERIAL PRIMARY KEY,
    id_chamado BIGINT NOT NULL REFERENCES chamados(id) ON DELETE CASCADE,
    id_usuario BIGINT NOT NULL REFERENCES users(id),
    mensagem TEXT NOT NULL,
    tipo_mensagem VARCHAR(20) NOT NULL DEFAULT 'TEXTO', 
    url_arquivo VARCHAR(255),
    nome_original_arquivo VARCHAR(255),
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Tabela de Monitoramento de Backups
CREATE TABLE logs_backup (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL,
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
    data_alerta TIMESTAMP, 
    frequencia VARCHAR(20), 
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', 
    id_usuario_criador BIGINT REFERENCES users(id) ON DELETE SET NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. Tabela de Participantes de Chamados
CREATE TABLE chamado_participantes (
    id BIGSERIAL PRIMARY KEY,
    id_chamado BIGINT NOT NULL REFERENCES chamados(id) ON DELETE CASCADE,
    id_usuario BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    papel VARCHAR(50) NOT NULL,
    status_convite VARCHAR(20) NOT NULL DEFAULT 'ACEITO', -- NOVO: 'PENDENTE' (aguardando aceite) ou 'ACEITO'
    data_adicao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id_chamado, id_usuario) 
);

-- 11. Tabela de Solicitações de Novos Colaboradores (Onboarding RH)
CREATE TABLE solicitacoes_rh (
    id BIGSERIAL PRIMARY KEY,
    empresa VARCHAR(50) NOT NULL,
    nome_completo VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    cargo VARCHAR(100) NOT NULL,
    id_departamento BIGINT REFERENCES departamentos(id) ON DELETE SET NULL,
    data_inicio DATE NOT NULL,
    id_maquina_sugerida BIGINT REFERENCES ativos(id) ON DELETE SET NULL,
    id_ex_colaborador_espelho BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', 
    motivo_reprovacao TEXT,
    id_usuario_abriu BIGINT NOT NULL REFERENCES users(id),
    id_usuario_respondeu BIGINT REFERENCES users(id) ON DELETE SET NULL,
    data_solicitacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_resposta TIMESTAMP NULL
);

-- 12. Tabela de Solicitações de Troca de Equipamentos
CREATE TABLE solicitacoes_equipamento (
    id BIGSERIAL PRIMARY KEY,
    id_ativo BIGINT NOT NULL REFERENCES ativos(id) ON DELETE CASCADE,
    id_usuario_abriu BIGINT NOT NULL REFERENCES users(id),
    motivo_substituicao TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', 
    motivo_resposta TEXT,
    id_usuario_respondeu BIGINT REFERENCES users(id) ON DELETE SET NULL,
    data_solicitacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_resposta TIMESTAMP NULL
);

-- 13. NOVA: Tabela de Mural / Avisos Gerais
CREATE TABLE avisos_gerais (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    conteudo TEXT NOT NULL,
    url_imagem VARCHAR(255),
    empresa_alvo VARCHAR(50) NOT NULL DEFAULT 'AMBAS', -- Permite avisar só Bag Cleaner ou só Engebag
    id_usuario_criador BIGINT REFERENCES users(id) ON DELETE SET NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP -- Se nulo, o aviso nunca expira
);

-- 14. NOVA: Tabela de Links Úteis
CREATE TABLE links_uteis (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    url_site TEXT NOT NULL,
    url_imagem VARCHAR(255),
    empresa_alvo VARCHAR(50) NOT NULL DEFAULT 'AMBAS',
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- CRIAÇÃO DE ÍNDICES PARA OTIMIZAÇÃO DE BUSCAS
-- ==============================================================================
CREATE INDEX idx_chamados_status ON chamados(status);
CREATE INDEX idx_chamados_tecnico ON chamados(id_tecnico_atribuido);
CREATE INDEX idx_vencimentos_data ON contratos_licencas(data_vencimento);
CREATE INDEX idx_ativos_tipo ON ativos(tipo_ativo);
CREATE INDEX idx_ativos_responsavel ON ativos(id_usuario_responsavel); 
CREATE INDEX idx_credenciais_tipo ON credenciais(tipo_credencial);
CREATE INDEX idx_credenciais_dono ON credenciais(id_usuario_dono); 
CREATE INDEX idx_mensagens_chamado ON mensagens_chamado(id_chamado);
CREATE INDEX idx_lembretes_status ON lembretes(status);
CREATE INDEX idx_participantes_chamado ON chamado_participantes(id_chamado);
CREATE INDEX idx_solicitacoes_rh_status ON solicitacoes_rh(status);
CREATE INDEX idx_avisos_expiracao ON avisos_gerais(data_expiracao);