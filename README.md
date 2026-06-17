<h1>Escopo Técnico - Gestão T.I. 2.0</h1>
Este documento descreve o escopo técnico, arquitetura e estratégia de banco de dados para o
novo sistema integrado de Gestão de T.I. 2.0, com base nas necessidades mapeadas e na
infraestrutura existente. <br>

<h3>1. Arquitetura e Stack Tecnológico</h3>

O sistema seguirá uma arquitetura cliente-servidor (SPA + REST API), garantindo alta
performance, segurança e reatividade.

● Frontend: React + Vite (build rápido e otimizado), TailwindCSS (estilização utilitária
baseada no design), shadcn/ui (componentes padronizados) e Axios/WebSocket.

● Backend: Java 21 com Spring Boot 3, Spring Security integrado com JWT (JSON Web
Token), Spring WebSocket (STOMP) para comunicação em tempo real e Spring Mail
(opcional para a fase 2).

● Banco de Dados e Infra: PostgreSQL (substituindo o MySQL legado) e Flyway (para
controle de versão e migrações do banco).

<h3>2. Detalhamento dos Módulos Principais</h3>

<h5>2.1. Visão Geral (Dashboard)</h5>
● KPIs em Tempo Real: Monitoramento de "Chamados Abertos", "% SLA Cumprido", "%
Backups com Sucesso" e "Credenciais a Expirar".

● Feed de Atividades: Lista contínua de chamados recentes (com indicadores visuais de
status) e alertas vitais do sistema.

<h5>2.2. Gestão de Chamados (Help Desk)</h5>
● Abertura e fila de trabalho com categorização, prioridade e upload de anexos.

● Gestão de SLA e cronômetros de prazos baseados na criticidade estipulada.

● Sistema de Chat em tempo real (via WebSocket) integrado diretamente na tela do chamado.

<h5>2.3. Automação SQL e Base de Conhecimento</h5>

● Automação SQL: Repositório centralizado e painel para execução controlada de scripts
(com logs de auditoria).

● Base de Conhecimento (KB): Catálogo central com links para documentações externas,
wikis e manuais.

<h5>2.4. Ativos, Credenciais e Backups</h5>

● Estoque e Patrimônio (CMDB): Cadastro detalhado de computadores, monitores,
impressoras, câmeras, etc.

● Credenciais: Cofre de senhas seguro e gestão de datas de vencimento de contratos e
domínios.

● Monitoramento de Backups: Logs e status (Sucesso/Falha) de rotinas de ERP, File
Servers e Bancos de Dados.
