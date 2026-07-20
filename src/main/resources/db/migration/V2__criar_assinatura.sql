CREATE TABLE assinatura (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario (id),
    plano VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_inicio DATE,
    data_expiracao DATE,
    data_cancelamento DATE,
    falhas_renovacao_consecutivas INT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_assinatura_usuario_id ON assinatura (usuario_id);
