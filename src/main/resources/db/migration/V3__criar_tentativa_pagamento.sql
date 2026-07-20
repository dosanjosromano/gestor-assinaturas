CREATE TABLE tentativa_pagamento (
    id UUID PRIMARY KEY,
    assinatura_id UUID NOT NULL REFERENCES assinatura (id),
    tipo VARCHAR(20) NOT NULL,
    tentativa_numero INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_tentativa_pagamento_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_tentativa_pagamento_assinatura_id ON tentativa_pagamento (assinatura_id);
