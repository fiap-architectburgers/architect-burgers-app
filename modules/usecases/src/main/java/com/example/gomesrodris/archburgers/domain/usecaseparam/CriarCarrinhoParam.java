package com.example.gomesrodris.archburgers.domain.usecaseparam;

import org.jetbrains.annotations.Nullable;

/**
 * Único parâmetro utilizado é nomeCliente: Para cliente não identificado, chamar pelo nome apenas para este pedido.
 * Para clientes cadastrados obtemos as credenciais a partir da identificação (login)
 */
public record CriarCarrinhoParam(
        @Nullable String nomeCliente
) {

}
