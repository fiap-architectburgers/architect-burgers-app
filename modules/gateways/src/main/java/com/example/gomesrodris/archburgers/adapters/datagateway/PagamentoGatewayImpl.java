package com.example.gomesrodris.archburgers.adapters.datagateway;

import com.example.gomesrodris.archburgers.domain.datagateway.PagamentoGateway;
import com.example.gomesrodris.archburgers.domain.datasource.PagamentoDataSource;
import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import org.springframework.stereotype.Service;

@Service
public class PagamentoGatewayImpl implements PagamentoGateway {
    private final PagamentoDataSource pagamentoDataSource;

    public PagamentoGatewayImpl(PagamentoDataSource pagamentoDataSource) {
        this.pagamentoDataSource = pagamentoDataSource;
    }

    @Override
    public Pagamento findPagamentoByPedido(Integer idPedido) {
        return pagamentoDataSource.findPagamentoByPedido(idPedido);
    }

    @Override
    public Pagamento salvarPagamento(Pagamento pagamento) {
        return pagamentoDataSource.salvarPagamento(pagamento);
    }

    @Override
    public void updateStatus(Pagamento pagamento) {
        pagamentoDataSource.updateStatus(pagamento);
    }

    @Override
    public void excluirPagamento(Integer idPedido) {
        pagamentoDataSource.deletePagamentoByIdPedido(idPedido);
    }
}
