package com.example.gomesrodris.archburgers.domain.datagateway;

import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;

import java.util.List;

public interface HistoricoPedidosGateway {
    void arquivarPedidos(List<Pedido>pedidosAntigos, List<Pagamento>pagamentosAntigos);
}
