package com.example.gomesrodris.archburgers.adapters.datagateway;

import com.example.gomesrodris.archburgers.domain.datagateway.PedidoGateway;
import com.example.gomesrodris.archburgers.domain.datasource.PedidoDataSource;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPedido;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoGatewayImpl implements PedidoGateway {
    private final PedidoDataSource pedidoDataSource;

    public PedidoGatewayImpl(PedidoDataSource pedidoDataSource) {
        this.pedidoDataSource = pedidoDataSource;
    }

    @Override
    public Pedido getPedido(int idPedido) {
        return pedidoDataSource.getPedido(idPedido);
    }

    @Override
    public Pedido savePedido(Pedido pedido) {
        return pedidoDataSource.savePedido(pedido);
    }

    @Override
    public List<Pedido> listPedidos(List<StatusPedido> filtroStatus, @Nullable LocalDateTime olderThan) {
        return pedidoDataSource.listPedidos(filtroStatus, olderThan);
    }

    @Override
    public void updateStatus(Pedido pedido) {
        pedidoDataSource.updateStatus(pedido);
    }

    @Override
    public void excluirPedido(int idPedido) { pedidoDataSource.deletePedido(idPedido); }
}
