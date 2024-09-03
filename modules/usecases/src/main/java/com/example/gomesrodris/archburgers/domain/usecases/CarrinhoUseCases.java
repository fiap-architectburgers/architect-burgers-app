package com.example.gomesrodris.archburgers.domain.usecases;

import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.datagateway.CarrinhoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ClienteGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.exception.DomainArgumentException;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarCarrinhoParam;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import com.example.gomesrodris.archburgers.domain.utils.StringUtils;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdCliente;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CarrinhoUseCases {

    private final CarrinhoGateway carrinhoGateway;
    private final ClienteGateway clienteGateway;
    private final ItemCardapioGateway itemCardapioGateway;
    private final Clock clock;

    private final RecuperarCarrinhoPolicy recuperarCarrinhoPolicy;

    public CarrinhoUseCases(CarrinhoGateway carrinhoGateway,
                            ClienteGateway clienteGateway,
                            ItemCardapioGateway itemCardapioGateway,
                            Clock clock) {
        this.carrinhoGateway = carrinhoGateway;
        this.clienteGateway = clienteGateway;
        this.itemCardapioGateway = itemCardapioGateway;
        this.clock = clock;

        this.recuperarCarrinhoPolicy = new RecuperarCarrinhoPolicy();
    }

    public Carrinho criarCarrinho(@NotNull CriarCarrinhoParam param, @NotNull UsuarioLogado usuarioLogado) {
        IdCliente idClienteLogado;
        if (usuarioLogado.autenticado()) {
            var clienteRegistrado = clienteGateway.getClienteByCpf(new Cpf(usuarioLogado.getCpf()));
            if (clienteRegistrado == null)
                throw new RuntimeException("Registro inconsistente! Usuario logado [" + usuarioLogado.getCpf() + "] não cadastrado na base");
            idClienteLogado = clienteRegistrado.id();
        } else {
            idClienteLogado = null;
        }

        if (idClienteLogado != null) {
            var carrinhoSalvo = recuperarCarrinhoPolicy.tryRecuperarCarrinho(idClienteLogado);
            if (carrinhoSalvo != null) {
                var itens = itemCardapioGateway.findByCarrinho(Objects.requireNonNull(carrinhoSalvo.id(), "Object from database should have ID"));
                return carrinhoSalvo.withItens(itens);
            }
        }

        Carrinho newCarrinho;
        if (idClienteLogado != null) {
            newCarrinho = Carrinho.newCarrinhoVazioClienteIdentificado(
                    idClienteLogado, clock.localDateTime());
        } else {
            if (StringUtils.isEmpty(param.nomeCliente()))
                throw new DomainArgumentException("Cliente não autenticado deve informar o nomeCliente");

            newCarrinho = Carrinho.newCarrinhoVazioClienteNaoIdentificado(param.nomeCliente(), clock.localDateTime());
        }

        return carrinhoGateway.salvarCarrinhoVazio(newCarrinho);
    }

    public Carrinho addItem(int idCarrinho, int idItemCardapio) {
        var carrinho = carrinhoGateway.getCarrinho(idCarrinho);
        if (carrinho == null) {
            throw new IllegalArgumentException("Carrinho invalido! " + idCarrinho);
        }

        var itemCardapio = itemCardapioGateway.findById(idItemCardapio);
        if (itemCardapio == null) {
            throw new IllegalArgumentException("Item cardapio invalido! " + idItemCardapio);
        }

        var currentItens = itemCardapioGateway.findByCarrinho(idCarrinho);
        carrinho = carrinho.withItens(currentItens);

        var newCarrinho = carrinho.adicionarItem(itemCardapio);

        ItemPedido newItem = newCarrinho.itens().getLast();
        if (newItem.itemCardapio().id() != idItemCardapio) {
            throw new IllegalStateException("Invalid state check! Last item should be the new. " + idItemCardapio + " - " + newItem);
        }

        carrinhoGateway.salvarItemCarrinho(newCarrinho, newItem);
        return newCarrinho;
    }

    public Carrinho deleteItem(int idCarrinho, int numSequencia) {
        var carrinho = carrinhoGateway.getCarrinho(idCarrinho);
        if (carrinho == null) {
            throw new IllegalArgumentException("Carrinho invalido! " + idCarrinho);
        }

        var currentItens = itemCardapioGateway.findByCarrinho(idCarrinho);
        carrinho = carrinho.withItens(currentItens);

        carrinho = carrinho.deleteItem(numSequencia);

        carrinhoGateway.deleteItensCarrinho(carrinho);
        for (ItemPedido item : carrinho.itens()) {
            carrinhoGateway.salvarItemCarrinho(carrinho, item);
        }

        return carrinho;
    }

    public Carrinho setObservacoes(int idCarrinho, String textoObservacao) {
        var carrinho = carrinhoGateway.getCarrinho(idCarrinho);
        if (carrinho == null) {
            throw new IllegalArgumentException("Carrinho invalido! " + idCarrinho);
        }

        var newCarrinho = carrinho.setObservacoes(textoObservacao);

        carrinhoGateway.updateObservacaoCarrinho(newCarrinho);

        var currentItens = itemCardapioGateway.findByCarrinho(idCarrinho);
        newCarrinho = newCarrinho.withItens(currentItens);

        return newCarrinho;
    }

    public Carrinho findCarrinho(int idCarrinho) {
        var carrinho = carrinhoGateway.getCarrinho(idCarrinho);
        if (carrinho == null) {
            throw new IllegalArgumentException("Carrinho invalido! " + idCarrinho);
        }

        var currentItens = itemCardapioGateway.findByCarrinho(idCarrinho);
        return carrinho.withItens(currentItens);
    }

    /**
     * Política do fluxo de compra: Se o cliente se identificou e há um carrinho salvo,
     * carrega para permitir continuar a compra
     */
    private class RecuperarCarrinhoPolicy {
        Carrinho tryRecuperarCarrinho(IdCliente idCliente) {
            return carrinhoGateway.getCarrinhoSalvoByCliente(idCliente);
        }
    }
}
