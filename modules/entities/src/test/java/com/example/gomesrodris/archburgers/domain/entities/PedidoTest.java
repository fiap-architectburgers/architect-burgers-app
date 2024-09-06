package com.example.gomesrodris.archburgers.domain.entities;

import com.example.gomesrodris.archburgers.domain.exception.DomainArgumentException;
import com.example.gomesrodris.archburgers.domain.valueobjects.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PedidoTest {

    private final List<ItemPedido> sampleItens = List.of(
            new ItemPedido(1, new ItemCardapio(9, TipoItemCardapio.LANCHE,
                    "Cheeseburger", "Hamburger com queijo", new ValorMonetario("19.90")))
    );

    @Test
    void validar() {
        var p = Pedido.pedidoRecuperado(123, null, "Cliente José",
                sampleItens, null, StatusPedido.RECEBIDO,
                IdFormaPagamento.DINHEIRO, LocalDateTime.now());

        var newP = p.validar();

        assertThat(newP.status()).isEqualTo(StatusPedido.PREPARACAO);
    }

    @Test
    void validar_statusInvalido() {
        var p = Pedido.pedidoRecuperado(123, null, "Cliente José",
                sampleItens, null, StatusPedido.PRONTO,
                IdFormaPagamento.DINHEIRO, LocalDateTime.now());

        assertThat(
                assertThrows(IllegalArgumentException.class, p::validar)
        ).hasMessage("Status invalido para validação do pedido: PRONTO");
    }

    @Test
    void cancelar() {
        var p = Pedido.pedidoRecuperado(123, null, "Cliente José",
                sampleItens, null, StatusPedido.RECEBIDO,
                IdFormaPagamento.DINHEIRO, LocalDateTime.now());

        var newP = p.cancelar();

        assertThat(newP.status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void setPronto() {
        var p = Pedido.pedidoRecuperado(123, null, "Cliente José",
                sampleItens, null, StatusPedido.PREPARACAO,
                IdFormaPagamento.DINHEIRO, LocalDateTime.now());

        var newP = p.setPronto();

        assertThat(newP.status()).isEqualTo(StatusPedido.PRONTO);
    }

    @Test
    void confirmarPagamento() {
        var p = Pedido.pedidoRecuperado(123, null, "Cliente José",
                sampleItens, null, StatusPedido.PAGAMENTO,
                IdFormaPagamento.DINHEIRO, LocalDateTime.now());

        var pagamento = new Pagamento(44, 123,
                IdFormaPagamento.DINHEIRO, StatusPagamento.FINALIZADO,
                new ValorMonetario("19.90"), LocalDateTime.now(),
                LocalDateTime.now(), null, null);

        var newP = p.confirmarPagamento(pagamento);

        assertThat(newP.status()).isEqualTo(StatusPedido.RECEBIDO);
    }

    @Test
    void confirmarPagamento_erroValor() {
        var p = Pedido.pedidoRecuperado(123, null, "Cliente José",
                sampleItens, null, StatusPedido.PAGAMENTO,
                IdFormaPagamento.DINHEIRO, LocalDateTime.now());

        var pagamento = new Pagamento(44, 123,
                IdFormaPagamento.DINHEIRO, StatusPagamento.FINALIZADO,
                new ValorMonetario("25"), LocalDateTime.now(),
                LocalDateTime.now(), null, null);

        var e = assertThrows(DomainArgumentException.class,() -> p.confirmarPagamento(pagamento));
        assertThat(e).hasMessage("Valor do pagamento não corresponde aos itens do pedido. Pedido=R$19.90, Pago=R$25.00");
    }
}