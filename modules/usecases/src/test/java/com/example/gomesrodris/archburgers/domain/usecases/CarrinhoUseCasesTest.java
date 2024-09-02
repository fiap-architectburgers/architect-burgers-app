package com.example.gomesrodris.archburgers.domain.usecases;

import com.example.gomesrodris.archburgers.domain.datagateway.CarrinhoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ClienteGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.entities.ItemCardapio;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarCarrinhoParam;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdCliente;
import com.example.gomesrodris.archburgers.domain.valueobjects.TipoItemCardapio;
import com.example.gomesrodris.archburgers.domain.valueobjects.ValorMonetario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarrinhoUseCasesTest {
    @Mock
    private CarrinhoGateway carrinhoGateway;
    @Mock
    private ClienteGateway clienteGateway;
    @Mock
    private ItemCardapioGateway itemCardapioGateway;

    @Mock
    private Clock clock;

    private CarrinhoUseCases carrinhoUseCases;

    @BeforeEach
    void setUp() {
        carrinhoUseCases = new CarrinhoUseCases(carrinhoGateway, clienteGateway, itemCardapioGateway, clock);
    }

    @Test
    void criarCarrinho_clienteIdentificado_carrinhoExistente() {
        when(carrinhoGateway.getCarrinhoSalvoByCliente(new IdCliente(123))).thenReturn(carrinhoSalvoCliente123);

        when(itemCardapioGateway.findByCarrinho(88)).thenReturn(List.of(
                new ItemPedido(1,
                        new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                )
        ));

        var result = carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam(123, null, null, null));
        assertThat(result).isEqualTo(carrinhoSalvoCliente123.withItens(List.of(
                        new ItemPedido(1,
                                new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                        )
                ))
        );
    }

    @Test
    void criarCarrinho_clienteIdentificado_novoCarrinho() throws Exception {
        when(carrinhoGateway.getCarrinhoSalvoByCliente(new IdCliente(123))).thenReturn(null);
        when(clock.localDateTime()).thenReturn(dateTime);

        when(carrinhoGateway.salvarCarrinhoVazio(carrinhoVazioCliente123)).thenReturn(
                carrinhoVazioCliente123.withId(99));

        when(clienteGateway.getClienteById(123)).thenReturn(cliente123);

        var result = carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam(123, null, null, null));
        assertThat(result).isEqualTo(carrinhoVazioCliente123.withId(99));
    }

    @Test
    void criarCarrinho_clienteNaoIdentificado_novoCarrinho() throws Exception {
        when(clock.localDateTime()).thenReturn(dateTime);

        when(carrinhoGateway.salvarCarrinhoVazio(carrinhoNaoIdentificado)).thenReturn(
                carrinhoNaoIdentificado.withId(101));

        var result = carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam(null, "João", null, null));
        assertThat(result).isEqualTo(carrinhoNaoIdentificado.withId(101));
    }

    @Test
    void addItemCarrinho() {
        Carrinho carrinhoInicial = Carrinho.carrinhoSalvoClienteIdentificado(
                88, new IdCliente(123), null, dateTime);

        when(carrinhoGateway.getCarrinho(88)).thenReturn(carrinhoInicial);

        when(itemCardapioGateway.findByCarrinho(88)).thenReturn(List.of(
                new ItemPedido(1,
                        new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                ),
                new ItemPedido(2,
                        new ItemCardapio(1001, TipoItemCardapio.BEBIDA, "Refrigerante", "Refrigerante", new ValorMonetario("5.00"))
                )
        ));

        when(itemCardapioGateway.findById(1002)).thenReturn(
                new ItemCardapio(1002, TipoItemCardapio.SOBREMESA, "Sundae", "Sundae", new ValorMonetario("9.40"))
        );

        var newCarrinho = carrinhoUseCases.addItem(88, 1002);

        assertThat(newCarrinho).isEqualTo(carrinhoInicial.withItens(List.of(
                new ItemPedido(1,
                        new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                ),
                new ItemPedido(2,
                        new ItemCardapio(1001, TipoItemCardapio.BEBIDA, "Refrigerante", "Refrigerante", new ValorMonetario("5.00"))
                ),
                new ItemPedido(3,
                        new ItemCardapio(1002, TipoItemCardapio.SOBREMESA, "Sundae", "Sundae", new ValorMonetario("9.40"))
                )
        )));

        verify(carrinhoGateway).salvarItemCarrinho(newCarrinho,
                new ItemPedido(3,
                        new ItemCardapio(1002, TipoItemCardapio.SOBREMESA, "Sundae", "Sundae", new ValorMonetario("9.40"))
                ));
    }

    @Test
    void deleteItem() {
        Carrinho carrinhoInicial = Carrinho.carrinhoSalvoClienteIdentificado(
                88, new IdCliente(123), null, dateTime);

        when(carrinhoGateway.getCarrinho(88)).thenReturn(carrinhoInicial);

        when(itemCardapioGateway.findByCarrinho(88)).thenReturn(List.of(
                new ItemPedido(1,
                        new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                ),
                new ItemPedido(2,
                        new ItemCardapio(1001, TipoItemCardapio.BEBIDA, "Refrigerante", "Refrigerante", new ValorMonetario("5.00"))
                ),
                new ItemPedido(3,
                        new ItemCardapio(1002, TipoItemCardapio.SOBREMESA, "Sundae", "Sundae", new ValorMonetario("9.40"))
                )
        ));

        ///
        var updated = carrinhoUseCases.deleteItem(88, 2);

        Carrinho expectedNewCarrinho = carrinhoInicial.withItens(List.of(
                new ItemPedido(1,
                        new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                ),
                new ItemPedido(2,
                        new ItemCardapio(1002, TipoItemCardapio.SOBREMESA, "Sundae", "Sundae", new ValorMonetario("9.40"))
                )
        ));
        assertThat(updated).isEqualTo(expectedNewCarrinho);

        verify(carrinhoGateway).deleteItensCarrinho(expectedNewCarrinho);
        verify(carrinhoGateway).salvarItemCarrinho(expectedNewCarrinho, new ItemPedido(1,
                new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
        ));
        verify(carrinhoGateway).salvarItemCarrinho(expectedNewCarrinho, new ItemPedido(2,
                new ItemCardapio(1002, TipoItemCardapio.SOBREMESA, "Sundae", "Sundae", new ValorMonetario("9.40"))
        ));
    }

    // // // Predefined test objects
    private final Cliente cliente123 = new Cliente(new IdCliente(123), "Cliente", new Cpf("12332112340"), "cliente123@example.com");
    private final Cliente clienteSemId = new Cliente(null, "Cliente", new Cpf("12332112340"), "cliente123@example.com");

    private final LocalDateTime dateTime = LocalDateTime.of(2024, 4, 29, 15, 30);

    private final Carrinho carrinhoSalvoCliente123 = Carrinho.carrinhoSalvoClienteIdentificado(
            88, new IdCliente(123), null, dateTime);

    private final Carrinho carrinhoNaoIdentificado = Carrinho.newCarrinhoVazioClienteNaoIdentificado("João", dateTime);

    private final Carrinho carrinhoVazioCliente123 = Carrinho.newCarrinhoVazioClienteIdentificado(new IdCliente(123), dateTime);

}