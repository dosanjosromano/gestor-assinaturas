CREATE UNIQUE INDEX uk_assinatura_usuario_ativa_ou_pendente
    ON assinatura (usuario_id)
    WHERE status IN ('ATIVA', 'AGUARDANDO_PAGAMENTO');
