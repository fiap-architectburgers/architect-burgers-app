package com.example.gomesrodris.archburgers.adapters.datagateway;

import com.example.gomesrodris.archburgers.domain.datagateway.HistoricoPedidosGateway;
import com.example.gomesrodris.archburgers.domain.datasource.HistoricoPedidosDataSource;
import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoricoPedidosGatewayImpl implements HistoricoPedidosGateway {
    private final HistoricoPedidosDataSource historicoPedidosDataSource;

    public HistoricoPedidosGatewayImpl(HistoricoPedidosDataSource historicoPedidosDataSource) {
        this.historicoPedidosDataSource = historicoPedidosDataSource;
    }

    @Override
    public void arquivarPedidos(List<Pedido> pedidosAntigos, List<Pagamento> pagamentosAntigos) {
        historicoPedidosDataSource.salvarHistoricoPedido(pedidosAntigos, pagamentosAntigos);
    }
}
