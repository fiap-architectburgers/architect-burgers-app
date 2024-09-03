package com.example.gomesrodris.archburgers.domain.datasource;

import com.example.gomesrodris.archburgers.domain.entities.Pagamento;

public interface PagamentoDataSource {
    Pagamento findPagamentoByPedido(Integer idPedido);
    Pagamento salvarPagamento(Pagamento pagamento);
    void updateStatus(Pagamento pagamento);
    void deletePagamentoByIdPedido(Integer idPedido);
}
