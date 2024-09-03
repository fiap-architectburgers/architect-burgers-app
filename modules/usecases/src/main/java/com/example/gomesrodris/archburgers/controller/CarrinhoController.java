package com.example.gomesrodris.archburgers.controller;

import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.datagateway.CarrinhoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ClienteGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarCarrinhoParam;
import com.example.gomesrodris.archburgers.domain.usecases.CarrinhoUseCases;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import org.jetbrains.annotations.NotNull;

public class CarrinhoController {
    private final CarrinhoUseCases carrinhoUseCases;

    public CarrinhoController(CarrinhoGateway carrinhoGateway,
                              ClienteGateway clienteGateway,
                              ItemCardapioGateway itemCardapioGateway,
                              Clock clock) {
        this.carrinhoUseCases = new CarrinhoUseCases(carrinhoGateway, clienteGateway, itemCardapioGateway, clock);
    }

    public Carrinho findCarrinho(int idCarrinho) {
        return carrinhoUseCases.findCarrinho(idCarrinho);
    }

    public Carrinho criarCarrinho(@NotNull CriarCarrinhoParam param, @NotNull UsuarioLogado usuarioLogado) {
        return carrinhoUseCases.criarCarrinho(param, usuarioLogado);
    }

    public Carrinho addItem(int idCarrinho, int idItemCardapio) {
        return carrinhoUseCases.addItem(idCarrinho, idItemCardapio);
    }

    public Carrinho deleteItem(int idCarrinho, int numSequencia) {
        return carrinhoUseCases.deleteItem(idCarrinho, numSequencia);
    }

    public Carrinho setObservacoes(int idCarrinho, String textoObservacao) {
        return carrinhoUseCases.setObservacoes(idCarrinho, textoObservacao);
    }


}
