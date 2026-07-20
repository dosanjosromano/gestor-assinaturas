CREATE TABLE usuario (
                         id UUID PRIMARY KEY,
                         nome VARCHAR(60) NOT NULL,
                         email VARCHAR(60) NOT NULL UNIQUE,
                         criado_em TIMESTAMP NOT NULL DEFAULT now()
);