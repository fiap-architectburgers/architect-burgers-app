package com.example.gomesrodris.archburgers.domain.usecases;

import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.PagamentoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.PedidoGateway;
import com.example.gomesrodris.archburgers.domain.entities.ItemCardapio;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.exception.DomainArgumentException;
import com.example.gomesrodris.archburgers.domain.external.FormaPagamento;
import com.example.gomesrodris.archburgers.domain.external.FormaPagamentoRegistry;
import com.example.gomesrodris.archburgers.domain.usecaseparam.DescricaoFormaPagamento;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdFormaPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPagamento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PagamentoUseCases {
    private final FormaPagamentoRegistry formaPagamentoRegistry;
    private final PagamentoGateway pagamentoGateway;
    private final PedidoGateway pedidoGateway;
    private final ItemCardapioGateway itemCardapioGateway;
    private final Clock clock;

    public PagamentoUseCases(FormaPagamentoRegistry formaPagamentoRegistry,
                             PagamentoGateway pagamentoGateway,
                             PedidoGateway pedidoGateway,
                             ItemCardapioGateway itemCardapioGateway,
                             Clock clock) {
        this.formaPagamentoRegistry = formaPagamentoRegistry;
        this.pagamentoGateway = pagamentoGateway;
        this.pedidoGateway = pedidoGateway;
        this.itemCardapioGateway = itemCardapioGateway;
        this.clock = clock;
    }

    public IdFormaPagamento validarFormaPagamento(String idFormaPagamento) throws DomainArgumentException {
        var formaPagamento = formaPagamentoRegistry.getFormaPagamento(new IdFormaPagamento(idFormaPagamento));
        return formaPagamento.id();
    }

    public Pagamento iniciarPagamento(Pedido pedido) {
        var formaPagamento = formaPagamentoRegistry.getFormaPagamento(pedido.formaPagamento());

        FormaPagamento.InfoPagamentoExterno infoPagamentoExterno;
        if (formaPagamento.isIntegracaoExterna()) {
            infoPagamentoExterno = formaPagamento.iniciarRegistroPagamento(pedido);
        } else {
            infoPagamentoExterno = new FormaPagamento.InfoPagamentoExterno(null, null);
        }

        var pagamento = Pagamento.registroInicial(
                Objects.requireNonNull(pedido.id(), "Pedido deve estar gravado para iniciar pagamento"),
                formaPagamento.id(),
                ItemCardapio.somarValores(pedido.itens().stream().map(ItemPedido::itemCardapio).toList()),
                clock.localDateTime(),
                infoPagamentoExterno.codigoPagamentoCliente(),
                infoPagamentoExterno.idPedidoSistemaExterno());

        return pagamentoGateway.salvarPagamento(pagamento);
    }

    public Pedido finalizarPagamento(int idPedido, String newIdPedidoSistemaExterno) {
        Pedido pedido = pedidoGateway.getPedido(idPedido);

        if (pedido == null) {
            throw new DomainArgumentException("Pedido invalido=" + idPedido);
        }

        Pagamento inicial = pagamentoGateway.findPagamentoByPedido(idPedido);

        if (inicial == null) {
            throw new DomainArgumentException("Pagamento nao encontrado. Pedido=" + idPedido);
        }

        pedido = pedido.withItens(itemCardapioGateway.findByPedido(idPedido));

        if (inicial.status() != StatusPagamento.PENDENTE) {
            throw new DomainArgumentException("Pagamento nao está Pendente. Pedido=" + idPedido);
        }

        LocalDateTime dataHoraPagamento = clock.localDateTime();

        Pagamento pagamentoFinalizado;
        if (newIdPedidoSistemaExterno != null && !newIdPedidoSistemaExterno.equals(inicial.idPedidoSistemaExterno())) {
            pagamentoFinalizado = inicial.finalizar(dataHoraPagamento, newIdPedidoSistemaExterno);
        } else {
            pagamentoFinalizado = inicial.finalizar(dataHoraPagamento);
        }

        var pedidoPago = pedido.confirmarPagamento(pagamentoFinalizado);

        pagamentoGateway.updateStatus(pagamentoFinalizado);

        pedidoGateway.updateStatus(pedidoPago);

        return pedidoPago;
    }

    public List<DescricaoFormaPagamento> listarFormasPagamento() {
        return formaPagamentoRegistry.listAll()
                .stream().map(formaPagamento -> new DescricaoFormaPagamento(formaPagamento.id(), formaPagamento.descricao()))
                .toList();
    }

    public Pagamento consultarPagamento(int idPedido) {
        return pagamentoGateway.findPagamentoByPedido(idPedido);
    }

    public void excluirPagamentoDoPedido(int idPedido) {
        pagamentoGateway.excluirPagamento(idPedido);
    }
}
