-- =====================================================================
-- Aula.net — script MySQL 8 do banco "aulanetdb"
-- Baseado no modelo "BD Aula.Net" (MySQL Workbench). Acréscimo: usuario.bio (RF05).
-- Opcional: com o perfil "mysql" o Hibernate cria/atualiza as tabelas sozinho
-- (spring.jpa.hibernate.ddl-auto=update). Use este script se preferir criar manualmente.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS aulanetdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aulanetdb;

CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    senha      VARCHAR(255) NOT NULL,              -- hash BCrypt
    cpf        CHAR(11) UNIQUE NULL,
    telefone   VARCHAR(20) NULL,
    papel      ENUM('ALUNO','PROFESSOR','ADMIN') NOT NULL,
    bio        VARCHAR(1000) NULL,
    INDEX idx_usuario_papel (papel)
);

CREATE TABLE disciplina (
    id_disciplina INT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE oferta_aula (
    id_oferta           INT AUTO_INCREMENT PRIMARY KEY,
    id_professor        INT NOT NULL,
    id_disciplina       INT NOT NULL,
    modalidade          ENUM('PRESENCIAL','ONLINE') NOT NULL,
    valor_hora          DECIMAL(10,2) NOT NULL,
    percentual_comissao DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (id_professor)  REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_disciplina) REFERENCES disciplina(id_disciplina),
    INDEX idx_oferta_disciplina (id_disciplina),
    INDEX idx_oferta_modalidade (modalidade),
    INDEX idx_oferta_valor (valor_hora)
);

CREATE TABLE agendamento (
    id_agendamento  INT AUTO_INCREMENT PRIMARY KEY,
    id_aluno        INT NOT NULL,
    id_oferta       INT NOT NULL,
    data_aula       DATE NOT NULL,
    horario_inicio  TIME NOT NULL,
    duracao_minutos INT NOT NULL,
    status          ENUM('SOLICITADO','CONFIRMADO','CANCELADO','CONCLUIDO') DEFAULT 'SOLICITADO',
    FOREIGN KEY (id_aluno)  REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_oferta) REFERENCES oferta_aula(id_oferta),
    INDEX idx_agendamento_data (data_aula),
    INDEX idx_agendamento_status (status)
);

CREATE TABLE avaliacao (
    id_avaliacao   INT AUTO_INCREMENT PRIMARY KEY,
    id_agendamento INT NOT NULL UNIQUE,
    id_aluno       INT NOT NULL,
    id_professor   INT NOT NULL,
    nota           INT NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario     TEXT NULL,
    FOREIGN KEY (id_agendamento) REFERENCES agendamento(id_agendamento),
    FOREIGN KEY (id_aluno)       REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_professor)   REFERENCES usuario(id_usuario)
);

CREATE TABLE pagamento (
    id_pagamento   INT AUTO_INCREMENT PRIMARY KEY,
    id_agendamento INT NOT NULL UNIQUE,
    valor_total    DECIMAL(10,2) NOT NULL,
    tipo           ENUM('A_VISTA','PARCELADO') NOT NULL,
    qtd_parcelas   INT DEFAULT 1,
    FOREIGN KEY (id_agendamento) REFERENCES agendamento(id_agendamento)
);

CREATE TABLE parcela (
    id_parcela      INT AUTO_INCREMENT PRIMARY KEY,
    id_pagamento    INT NOT NULL,
    numero_parcela  INT NOT NULL,
    valor           DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    status          ENUM('PENDENTE','PAGO','ATRASADO') DEFAULT 'PENDENTE',
    valor_juros     DECIMAL(10,2) DEFAULT 0.00,
    valor_multa     DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (id_pagamento) REFERENCES pagamento(id_pagamento),
    INDEX idx_parcela_status_venc (status, data_vencimento)
);

CREATE TABLE repasse (
    id_repasse     INT AUTO_INCREMENT PRIMARY KEY,
    id_pagamento   INT NOT NULL UNIQUE,
    id_professor   INT NOT NULL,
    valor_bruto    DECIMAL(10,2) NOT NULL,
    valor_comissao DECIMAL(10,2) NOT NULL,
    valor_liquido  DECIMAL(10,2) NOT NULL,
    status         ENUM('PENDENTE','REALIZADO') DEFAULT 'PENDENTE',
    FOREIGN KEY (id_pagamento) REFERENCES pagamento(id_pagamento),
    FOREIGN KEY (id_professor) REFERENCES usuario(id_usuario)
);
