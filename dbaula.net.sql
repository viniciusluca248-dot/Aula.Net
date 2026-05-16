-- 1. Criando o banco de dados novo
CREATE DATABASE IF NOT EXISTS AulaNetDB;
USE AulaNetDB;

-- 2. Tabela Pessoa (centralizando pra não ter chabu de duplicação)
CREATE TABLE Pessoa (
    idPessoa INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    nascimento DATE,
    cpf VARCHAR(14) UNIQUE, 
    telefone VARCHAR(20),   
    endereco VARCHAR(255),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf CHAR(2),
    cep VARCHAR(10)
);

-- 3. Tabela Servico
CREATE TABLE Servico (
    idServico INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    status VARCHAR(20)
);

-- 4. Tabela Prestador
CREATE TABLE Prestador (
    idPrestador INT AUTO_INCREMENT PRIMARY KEY,
    fkIdPessoa INT NOT NULL,
    FOREIGN KEY (fkIdPessoa) REFERENCES Pessoa(idPessoa)
);

-- 5. Tabela Prestador de Servicos
CREATE TABLE PrestadorServico (
    idPrestServico INT AUTO_INCREMENT PRIMARY KEY,
    fkIdPrestador INT NOT NULL,
    fkIdServico INT NOT NULL,
    valor DECIMAL(10,2),
    FOREIGN KEY (fkIdPrestador) REFERENCES Prestador(idPrestador),
    FOREIGN KEY (fkIdServico) REFERENCES Servico(idServico)
);

-- 6. Tabela Contrato
CREATE TABLE Contrato (
    idContrato INT AUTO_INCREMENT PRIMARY KEY,
    fkIdPrestServico INT NOT NULL,
    fkIdPessoa INT NOT NULL, 
    valor DECIMAL(10,2),
    inicio DATE,
    fim DATE,
    status VARCHAR(20),
    data_reajuste DATE,
    FOREIGN KEY (fkIdPrestServico) REFERENCES PrestadorServico(idPrestServico),
    FOREIGN KEY (fkIdPessoa) REFERENCES Pessoa(idPessoa)
);

-- 7. Tabela Aluno Contrato
CREATE TABLE AlunoContrato (
    idAlunoContrato INT AUTO_INCREMENT PRIMARY KEY,
    fkIdContrato INT NOT NULL, 
    fkIdPessoa INT NOT NULL,   
    FOREIGN KEY (fkIdContrato) REFERENCES Contrato(idContrato),
    FOREIGN KEY (fkIdPessoa) REFERENCES Pessoa(idPessoa)
);

-- 8. Tabela Pagamento
CREATE TABLE Pagamento (
    idPagamento INT AUTO_INCREMENT PRIMARY KEY,
    fkIdContrato INT NOT NULL,
    data DATE,
    valor DECIMAL(10,2),
    especie VARCHAR(50), 
    FOREIGN KEY (fkIdContrato) REFERENCES Contrato(idContrato)
);