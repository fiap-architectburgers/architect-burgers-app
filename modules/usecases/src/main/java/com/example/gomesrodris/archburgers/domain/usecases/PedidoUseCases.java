package com.example.gomesrodris.archburgers.domain.usecases;

import com.example.gomesrodris.archburgers.domain.datagateway.CarrinhoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.HistoricoPedidosGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.PedidoGateway;
import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.external.PainelPedidos;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarPedidoParam;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import com.example.gomesrodris.archburgers.domain.utils.StringUtils;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPedido;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class PedidoUseCases {
    private final PedidoGateway pedidoGateway;
    private final CarrinhoGateway carrinhoGateway;
    private final ItemCardapioGateway itemCardapioGateway;
    private final PagamentoUseCases pagamentoUseCases;
    private final HistoricoPedidosGateway historicoPedidosGateway;
    private final Clock clock;
    private final PainelPedidos painelPedidos;

    public PedidoUseCases(PedidoGateway pedidoGateway, CarrinhoGateway carrinhoGateway,
                          ItemCardapioGateway itemCardapioGateway,
                          PagamentoUseCases pagamentoUseCases,
                          HistoricoPedidosGateway historicoPedidosGateway,
                          Clock clock, PainelPedidos painelPedidos) {
        this.pedidoGateway = pedidoGateway;
        this.carrinhoGateway = carrinhoGateway;
        this.itemCardapioGateway = itemCardapioGateway;
        this.pagamentoUseCases = pagamentoUseCases;
        this.historicoPedidosGateway = historicoPedidosGateway;
        this.clock = clock;
        this.painelPedidos = painelPedidos;
    }

    public Pedido criarPedido(CriarPedidoParam param) {
        if (param == null)
            throw new IllegalArgumentException("Parameter missing");
        if (param.idCarrinho() == null)
            throw new IllegalArgumentException("idCarrinho missing");
        if (StringUtils.isEmpty(param.formaPagamento()))
            throw new IllegalArgumentException("formaPagamento missing");

        var formaPagamento = pagamentoUseCases.validarFormaPagamento(param.formaPagamento());

        var carrinho = carrinhoGateway.getCarrinho(param.idCarrinho());
        if (carrinho == null) {
            throw new IllegalArgumentException("Invalid idCarrinho " + param.idCarrinho());
        }

        var itens = itemCardapioGateway.findByCarrinho(param.idCarrinho());

        var pedido = Pedido.novoPedido(carrinho.idClienteIdentificado(), carrinho.nomeClienteNaoIdentificado(),
                itens, carrinho.observacoes(),
                formaPagamento, clock.localDateTime());

        Pedido saved = pedidoGateway.savePedido(pedido);

        pagamentoUseCases.iniciarPagamento(saved);

        carrinhoGateway.deleteCarrinho(carrinho);

        return saved;
    }

    public List<Pedido> listarPedidosByStatus(@Nullable StatusPedido filtroStatus) {
        if (filtroStatus == null) {
            throw new IllegalArgumentException("Obrigatório informar um filtro");
        }

        var pedidos = pedidoGateway.listPedidos(List.of(filtroStatus), null);
        return pedidos.stream().map(p -> {
            var itens = itemCardapioGateway.findByPedido(Objects.requireNonNull(p.id(), "Expected pedidos to have ID"));
            return p.withItens(itens);
        }).toList();
    }

    public List<Pedido> listarPedidosComAtraso() {
        var now = clock.localDateTime();
        var olderThan = now.minusMinutes(20);

        var pedidos = pedidoGateway.listPedidos(List.of(StatusPedido.RECEBIDO, StatusPedido.PREPARACAO), olderThan);
        return pedidos.stream().map(p -> {
            var itens = itemCardapioGateway.findByPedido(Objects.requireNonNull(p.id(), "Expected pedidos to have ID"));
            return p.withItens(itens);
        }).toList();
    }

    public List<Pedido> listarPedidosAtivos() {
        var pedidos = pedidoGateway.listPedidos(List.of(
                StatusPedido.PAGAMENTO, StatusPedido.RECEBIDO, StatusPedido.PREPARACAO, StatusPedido.PRONTO
        ), null);
        return pedidos.stream().map(p -> {
            var itens = itemCardapioGateway.findByPedido(Objects.requireNonNull(p.id(), "Expected pedidos to have ID"));
            return p.withItens(itens);
        }).toList();
    }

    public Pedido validarPedido(Integer idPedido) {
        return loadAndApply(idPedido, Pedido::validar);
    }

    public Pedido cancelarPedido(Integer idPedido) {
        return loadAndApply(idPedido, Pedido::cancelar);
    }

    public Pedido setPronto(Integer idPedido) {
        Pedido updated = loadAndApply(idPedido, Pedido::setPronto);
        painelPedidos.notificarPedidoPronto(updated);
        return updated;
    }

    public Pedido finalizarPedido(Integer idPedido) {
        return loadAndApply(idPedido, Pedido::finalizar);
    }

    private @NotNull Pedido loadAndApply(Integer idPedido, Function<Pedido, Pedido> update) {
        var pedido = pedidoGateway.getPedido(Objects.requireNonNull(idPedido, "ID não pode ser null"));

        if (pedido == null) {
            throw new IllegalArgumentException("Pedido não encontrado: " + idPedido);
        }

        var atualizado = update.apply(pedido);
        pedidoGateway.updateStatus(atualizado);

        var itens = itemCardapioGateway.findByPedido(idPedido);

        return atualizado.withItens(itens);
    }

    public void arquivarPedidos(Integer dias)  {

        List<Pedido>  pedidosAntigos = listarPedidosAntigos(dias);

        List<Pagamento>  pagamentosAntigos = new ArrayList<>();
        for (Pedido pedidoAntigo : pedidosAntigos){
            pagamentosAntigos.add(pagamentoUseCases.consultarPagamento(pedidoAntigo.id()));
        }
        historicoPedidosGateway.arquivarPedidos(pedidosAntigos, pagamentosAntigos);

        for (Pedido pedidoAntigo : pedidosAntigos){
            pagamentoUseCases.excluirPagamentoDoPedido(pedidoAntigo.id());
            excluirPedido(pedidoAntigo.id());
        }
    }

    private List<Pedido> listarPedidosAntigos(Integer dias) {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(dias);

        var pedidos = pedidoGateway.listPedidos(Arrays.stream(StatusPedido.values()).toList(), localDateTime);

        return pedidos.stream().map(p -> {
            var itens = itemCardapioGateway.findByPedido(Objects.requireNonNull(p.id(), "Expected pedidos to have ID"));
            return p.withItens(itens);
        }).toList();
    }

    public void excluirPedido(Integer idPedido)  {
        pedidoGateway.excluirPedido(idPedido);
    }
}
