package com.example.gomesrodris.archburgers.di;

import com.example.gomesrodris.archburgers.adapters.externalsystem.ProvedorAutenticacaoCognito;
import com.example.gomesrodris.archburgers.adapters.pagamento.MercadoPagoGateway;
import com.example.gomesrodris.archburgers.adapters.presenters.QrCodePresenter;
import com.example.gomesrodris.archburgers.controller.*;
import com.example.gomesrodris.archburgers.domain.datagateway.*;
import com.example.gomesrodris.archburgers.domain.external.FormaPagamentoRegistry;
import com.example.gomesrodris.archburgers.domain.external.PainelPedidos;
import com.example.gomesrodris.archburgers.domain.usecases.PagamentoUseCases;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DomainServiceBeans {

    @Bean
    Clock clock() {
        return new Clock();
    }

    @Bean
    public CardapioController cardapioController(ItemCardapioGateway itemCardapioGateway) {
        return new CardapioController(itemCardapioGateway);
    }

    @Bean
    public ClienteController clienteController(ClienteGateway clienteGateway,
                                               ProvedorAutenticacaoCognito provedorAutenticacaoCognito) {
        return new ClienteController(clienteGateway, provedorAutenticacaoCognito);
    }

    @Bean
    public FormaPagamentoRegistry formaPagamentoRegistry(MercadoPagoGateway mercadoPagoGateway) {
        return new FormaPagamentoRegistry(List.of(
                mercadoPagoGateway
        ));
    }

    @Bean
    public CarrinhoController carrinhoController(CarrinhoGateway carrinhoGateway,
                                                 ClienteGateway clienteGateway,
                                                 ItemCardapioGateway itemCardapioGateway,
                                                 Clock clock) {
        return new CarrinhoController(carrinhoGateway, clienteGateway, itemCardapioGateway, clock);
    }

    @Bean
    public PedidoController pedidoController(CarrinhoGateway carrinhoGateway,
                                             ItemCardapioGateway itemCardapioGateway,
                                             PedidoGateway pedidoGateway,
                                             ClienteGateway clienteGateway,
                                             PagamentoUseCases pagamentoUseCases,
                                             Clock clock,
                                             PainelPedidos painelPedidos) {
        return new PedidoController(pedidoGateway, carrinhoGateway, itemCardapioGateway,
                clienteGateway, pagamentoUseCases, clock, painelPedidos);
    }

    @Bean
    public PagamentoUseCases pagamentoUseCases(FormaPagamentoRegistry formaPagamentoRegistry,
                                                 PagamentoGateway pagamentoGateway,
                                                 PedidoGateway pedidoGateway,
                                                 ItemCardapioGateway itemCardapioGateway,
                                                 Clock clock) {
        return new PagamentoUseCases(formaPagamentoRegistry, pagamentoGateway,
                pedidoGateway, itemCardapioGateway, clock);
    }

    @Bean
    public PagamentoController pagamentoController(FormaPagamentoRegistry formaPagamentoRegistry,
                                                   PagamentoGateway pagamentoGateway,
                                                   PedidoGateway pedidoGateway,
                                                   ItemCardapioGateway itemCardapioGateway,
                                                   Clock clock) {
        return new PagamentoController(formaPagamentoRegistry, pagamentoGateway,
                pedidoGateway, itemCardapioGateway, clock);
    }

    @Bean
    public QrCodePresenter qrCodePresenter() {
        return new QrCodePresenter();
    }
}
