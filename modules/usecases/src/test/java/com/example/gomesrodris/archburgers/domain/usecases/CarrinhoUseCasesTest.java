package com.example.gomesrodris.archburgers.domain.usecases;

import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.datagateway.CarrinhoGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ClienteGateway;
import com.example.gomesrodris.archburgers.domain.datagateway.ItemCardapioGateway;
import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.entities.ItemCardapio;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.exception.DomainArgumentException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
        when(clienteGateway.getClienteByCpf(new Cpf("12332112340"))).thenReturn(cliente123);

        when(carrinhoGateway.getCarrinhoSalvoByCliente(new IdCliente(123))).thenReturn(carrinhoSalvoCliente123);

        when(itemCardapioGateway.findByCarrinho(88)).thenReturn(List.of(
                new ItemPedido(1,
                        new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                )
        ));

        UsuarioLogado usuarioLogado = mockUsuarioLogado("12332112340");

        var result = carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam(null), usuarioLogado);

        assertThat(result).isEqualTo(carrinhoSalvoCliente123.withItens(List.of(
                        new ItemPedido(1,
                                new ItemCardapio(1000, TipoItemCardapio.LANCHE, "Hamburger", "Hamburger", new ValorMonetario("25.90"))
                        )
                ))
        );
    }

    @Test
    void criarCarrinho_clienteIdentificado_novoCarrinho() throws Exception {
        when(clienteGateway.getClienteByCpf(new Cpf("12332112340"))).thenReturn(cliente123);

        when(carrinhoGateway.getCarrinhoSalvoByCliente(new IdCliente(123))).thenReturn(null);
        when(clock.localDateTime()).thenReturn(dateTime);

        when(carrinhoGateway.salvarCarrinhoVazio(carrinhoVazioCliente123)).thenReturn(
                carrinhoVazioCliente123.withId(99));

        UsuarioLogado usuarioLogado = mockUsuarioLogado("12332112340");

        var result = carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam(null), usuarioLogado);
        assertThat(result).isEqualTo(carrinhoVazioCliente123.withId(99));
    }

    @Test
    void criarCarrinho_clienteNaoIdentificado_novoCarrinho() throws Exception {
        when(clock.localDateTime()).thenReturn(dateTime);

        when(carrinhoGateway.salvarCarrinhoVazio(carrinhoNaoIdentificado)).thenReturn(
                carrinhoNaoIdentificado.withId(101));

        UsuarioLogado usuarioNaoLogado = mockUsuarioLogado(null);

        var result = carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam("João"), usuarioNaoLogado);
        assertThat(result).isEqualTo(carrinhoNaoIdentificado.withId(101));
    }

    @Test
    void criarCarrinho_clienteNaoIdentificado_erroNomeNaoInformado() throws Exception {
        UsuarioLogado usuarioNaoLogado = mockUsuarioLogado(null);

        assertThat(
                assertThrows(DomainArgumentException.class, () ->
                        carrinhoUseCases.criarCarrinho(new CriarCarrinhoParam(""), usuarioNaoLogado))
        ).hasMessage("Cliente não autenticado deve informar o nomeCliente");
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

    private UsuarioLogado mockUsuarioLogado(String cpf) {
        UsuarioLogado usuarioLogado = mock(UsuarioLogado.class);
        if (cpf == null) {
            when(usuarioLogado.autenticado()).thenReturn(false);
        } else {
            when(usuarioLogado.getCpf()).thenReturn(cpf);
            when(usuarioLogado.autenticado()).thenReturn(true);
        }
        return usuarioLogado;
    }

    // // // Predefined test objects
    private final Cliente cliente123 = new Cliente(new IdCliente(123), "Cliente", new Cpf("12332112340"), "cliente123@example.com");

    private final LocalDateTime dateTime = LocalDateTime.of(2024, 4, 29, 15, 30);

    private final Carrinho carrinhoSalvoCliente123 = Carrinho.carrinhoSalvoClienteIdentificado(
            88, new IdCliente(123), null, dateTime);

    private final Carrinho carrinhoNaoIdentificado = Carrinho.newCarrinhoVazioClienteNaoIdentificado("João", dateTime);

    private final Carrinho carrinhoVazioCliente123 = Carrinho.newCarrinhoVazioClienteIdentificado(new IdCliente(123), dateTime);

}