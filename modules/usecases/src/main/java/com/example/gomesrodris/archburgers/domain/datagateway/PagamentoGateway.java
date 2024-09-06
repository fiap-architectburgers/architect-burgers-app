package com.example.gomesrodris.archburgers.domain.datagateway;

import com.example.gomesrodris.archburgers.domain.entities.Pagamento;

public interface PagamentoGateway {
    Pagamento findPagamentoByPedido(Integer idPedido);
    Pagamento salvarPagamento(Pagamento pagamento);
    void updateStatus(Pagamento pagamento);
    void excluirPagamento(Integer idPedido);
}
