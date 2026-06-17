-- ==============================================================================
-- INSERÇÃO DE DADOS INICIAIS (DEPARTAMENTOS E USUÁRIOS)
-- ==============================================================================

-- 1. Inserir Departamentos Básicos
INSERT INTO departamentos (nome) VALUES ('Tecnologia da Informação');
INSERT INTO departamentos (nome) VALUES ('Recursos Humanos');
INSERT INTO departamentos (nome) VALUES ('Comercial');
INSERT INTO departamentos (nome) VALUES ('Expedição');

-- 2. Inserir Usuários
-- A senha inserida abaixo é '123456' criptografada usando o padrão BCrypt.
-- Quando o Spring Security for configurado, ele conseguirá validar o login perfeitamente.

INSERT INTO users (nome, email, senha, role, id_departamento, ativo) VALUES
(
    'Administrador do Sistema', 
    'admin@engebag.com.br', 
    '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 
    'ADMIN', 
    1, -- ID 1 = T.I.
    true
),
(
    'Técnico de Suporte (Analista)', 
    'info@engebag.com.br', 
    '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 
    'TECNICO', 
    1, -- ID 1 = T.I.
    true
),
(
    'João (Usuário Comum)', 
    'usuario@engebag.com.br', 
    '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 
    'USER', 
    2, -- ID 2 = RH
    true
);