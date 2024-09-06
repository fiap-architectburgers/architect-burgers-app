package com.example.gomesrodris.archburgers.domain.datasource;

import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;

import java.util.List;

public interface HistoricoPedidosDataSource {
    public void salvarHistoricoPedido(List<Pedido> pedidosAntigos, List<Pagamento>pagamentosAntigos);
}
