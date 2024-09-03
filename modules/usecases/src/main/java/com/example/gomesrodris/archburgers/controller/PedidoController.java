package com.example.gomesrodris.archburgers.controller;

import com.example.gomesrodris.archburgers.domain.datagateway.CarrinhoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.HistoricoPedidosGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.PedidoGateway;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.external.PainelPedidos;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarPedidoParam;
import com.example.gomesrodris.archburgers.domain.usecases.PagamentoUseCases;
import com.example.gomesrodris.archburgers.domain.usecases.PedidoUseCases;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPedido;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PedidoController {
    private final PedidoUseCases pedidoUseCases;

    public PedidoController(PedidoGateway pedidoGateway, CarrinhoGateway carrinhoGateway,
                            ItemCardapioGateway itemCardapioGateway,
                            PagamentoUseCases pagamentoUseCases,
                            HistoricoPedidosGateway historicoPedidosGateway,
                            Clock clock, PainelPedidos painelPedidos) {
        pedidoUseCases = new PedidoUseCases(pedidoGateway, carrinhoGateway, itemCardapioGateway,
                pagamentoUseCases, historicoPedidosGateway, clock, painelPedidos);
    }

    public Pedido criarPedido(CriarPedidoParam param) {
        return pedidoUseCases.criarPedido(param);
    }

    public List<Pedido> listarPedidosByStatus(@Nullable StatusPedido filtroStatus) {
        return pedidoUseCases.listarPedidosByStatus(filtroStatus);
    }

    public List<Pedido> listarPedidosComAtraso() {
        return pedidoUseCases.listarPedidosComAtraso();
    }

    /**
     * Lista com todos os status exceto Finalizado e Cancelado
     */
    public List<Pedido> listarPedidosAtivos() {
        return pedidoUseCases.listarPedidosAtivos();
    }

    public Pedido validarPedido(Integer idPedido) {
        return pedidoUseCases.validarPedido(idPedido);
    }

    public Pedido cancelarPedido(Integer idPedido) {
        return pedidoUseCases.cancelarPedido(idPedido);
    }

    public Pedido setPronto(Integer idPedido) {
        return pedidoUseCases.setPronto(idPedido);
    }

    public Pedido finalizarPedido(Integer idPedido) {
        return pedidoUseCases.finalizarPedido(idPedido);
    }

    public void arquivarPedidos(Integer dias) { pedidoUseCases.arquivarPedidos(dias); }
}
