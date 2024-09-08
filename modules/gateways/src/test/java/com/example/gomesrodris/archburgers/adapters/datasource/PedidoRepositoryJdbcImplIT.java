package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.entities.ItemCardapio;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdFormaPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPedido;
import com.example.gomesrodris.archburgers.domain.valueobjects.TipoItemCardapio;
import com.example.gomesrodris.archburgers.domain.valueobjects.ValorMonetario;
import com.example.gomesrodris.archburgers.testUtils.RealDatabaseTestHelper;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database Integration Tests
 */
class PedidoRepositoryJdbcImplIT {
    private static RealDatabaseTestHelper realDatabase;
    private DatabaseConnection databaseConnection;

    private PedidoRepositoryJdbcImpl repository;

    @BeforeAll
    static void beforeAll() throws Exception {
        realDatabase = new RealDatabaseTestHelper();
        realDatabase.beforeAll();
    }

    @AfterAll
    static void afterAll() {
        realDatabase.afterAll();
    }

    @BeforeEach
    void setUp() {
        databaseConnection = realDatabase.getConnectionPool();
        repository = new PedidoRepositoryJdbcImpl(databaseConnection);
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
    }

    @Test
    void getPedido() {
        var pedido = repository.getPedido(1);

        assertThat(pedido).isEqualTo(Pedido.pedidoRecuperado(1, null, "Cliente Erasmo",
                Collections.emptyList(), "Sem cebola", StatusPedido.RECEBIDO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 18, 15, 30, 12))
        );
    }

    @Test
    void savePedido() throws SQLException {
        var pedido = Pedido.novoPedido(null, "Wanderley", List.of(
                        new ItemPedido(1,
                                new ItemCardapio(3, TipoItemCardapio.ACOMPANHAMENTO, "Batatas Fritas P", "Batatas fritas porção pequena", new ValorMonetario("8.00"))
                        ),
                        new ItemPedido(2,
                                new ItemCardapio(6, TipoItemCardapio.BEBIDA, "Chá gelado", "Chá gelado com limão, feito na casa", new ValorMonetario("6.00"))
                        )
                ), "Batatas com muito sal",
                IdFormaPagamento.DINHEIRO, LocalDateTime.of(2024, 5, 18, 15, 30, 12));

        var saved = repository.savePedido(pedido);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.id()).isGreaterThan(1);

        assertThat(saved).isEqualTo(pedido.withId(saved.id()));

        var loaded = repository.getPedido(saved.id());
        assertThat(loaded).isEqualTo(saved.withItens(List.of()));

        try (var conn = databaseConnection.jdbcConnection();
             var stmt = conn.prepareStatement("select item_cardapio_id,num_sequencia from pedido_item where pedido_id = ?")) {

            stmt.setInt(1, saved.id());
            var rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("item_cardapio_id")).isEqualTo(3);
            assertThat(rs.getInt("num_sequencia")).isEqualTo(1);

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("item_cardapio_id")).isEqualTo(6);
            assertThat(rs.getInt("num_sequencia")).isEqualTo(2);

            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void listPedidosByStatus_found() {
        var pedidos = repository.listPedidos(List.of(StatusPedido.RECEBIDO, StatusPedido.PRONTO), null);

        assertThat(pedidos.size()).isGreaterThanOrEqualTo(2);

        assertThat(pedidos).contains(Pedido.pedidoRecuperado(1, null, "Cliente Erasmo",
                Collections.emptyList(), "Sem cebola", StatusPedido.RECEBIDO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 18, 15, 30, 12))
        );

        assertThat(pedidos).contains(Pedido.pedidoRecuperado(2, null, "Paulo Sérgio",
                Collections.emptyList(), null, StatusPedido.PRONTO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 18, 15, 30, 12))
        );
    }

    @Test
    void listPedidosByStatus_verificaOrdem() {
        var pedidos = repository.listPedidos(List.of(
                StatusPedido.PREPARACAO, StatusPedido.RECEBIDO, StatusPedido.PRONTO, StatusPedido.PAGAMENTO), null);

        assertThat(pedidos.size()).isGreaterThanOrEqualTo(3);

        var indexRecebido = pedidos.indexOf(Pedido.pedidoRecuperado(1, null, "Cliente Erasmo",
                Collections.emptyList(), "Sem cebola", StatusPedido.RECEBIDO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 18, 15, 30, 12))
        );
        assertThat(indexRecebido).isGreaterThanOrEqualTo(0);

        var indexProntoNewer = pedidos.indexOf(Pedido.pedidoRecuperado(2, null, "Paulo Sérgio",
                Collections.emptyList(), null, StatusPedido.PRONTO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 18, 15, 30, 12))
        );
        assertThat(indexProntoNewer).isGreaterThanOrEqualTo(0);

        var indexEmPreparacao = pedidos.indexOf(Pedido.pedidoRecuperado(3, null, "Vanusa",
                Collections.emptyList(), null, StatusPedido.PREPARACAO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 17, 15, 30, 12))
        );
        assertThat(indexEmPreparacao).isGreaterThanOrEqualTo(0);

        var indexProntoOlder = pedidos.indexOf(Pedido.pedidoRecuperado(4, null, "Ronnie",
                Collections.emptyList(), null, StatusPedido.PRONTO,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 17, 14, 30, 12))
        );
        assertThat(indexProntoOlder).isGreaterThanOrEqualTo(0);

        // Regras de ordenação:
        // Pronto > Em Preparação > Recebido > Em Pagamento
        // Mais antigos primeiro
        assertThat(indexProntoOlder).isLessThan(indexProntoNewer);
        assertThat(indexProntoNewer).isLessThan(indexEmPreparacao);
        assertThat(indexEmPreparacao).isLessThan(indexRecebido);
    }


    @Test
    void listPedidosByStatusAndOlderThanTime() {
        // NOT older than ref time
        var p1 = repository.savePedido(Pedido.novoPedido(null, "Wanderley",
                sampleItens, null,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 19, 10, 30, 12)));

        // OLDER than ref time
        var p2 = repository.savePedido(Pedido.novoPedido(null, "Carlinhos",
                sampleItens, null,
                IdFormaPagamento.DINHEIRO,
                LocalDateTime.of(2024, 5, 19, 10, 5, 10)));

        var pedidos = repository.listPedidos(List.of(StatusPedido.PAGAMENTO),
                LocalDateTime.of(2024, 5, 19, 10, 10, 0));

        assertThat(pedidos).doesNotContain(p1);
        assertThat(pedidos).contains(p2.withItens(Collections.emptyList())); // Itens are not present when only the root object is loaded
    }

    @Test
    void listPedidosByStatus_notFound() {
        var pedidos = repository.listPedidos(List.of(StatusPedido.CANCELADO), null);

        assertThat(pedidos).hasSize(0);
    }

    @Test
    void updateStatus_withoutIdPagamento() throws SQLException {
        var pedido = Pedido.novoPedido(null, "Wanderley", sampleItens, "Lanche sem cebola",
                IdFormaPagamento.DINHEIRO, LocalDateTime.of(2024, 5, 18, 15, 30, 12));

        var saved = repository.savePedido(pedido);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.id()).isGreaterThan(1);

        String statusBefore = readStatusFromDb(saved.id());
        assertThat(statusBefore).isEqualTo("PAGAMENTO");

        repository.updateStatus(saved.cancelar());

        String statusAfter = readStatusFromDb(saved.id());
        assertThat(statusAfter).isEqualTo("CANCELADO");

        delete(saved.id());
    }

    private String readStatusFromDb(int idPedido) throws SQLException {
        try (var conn = databaseConnection.jdbcConnection();
             var stmt = conn.prepareStatement("select status from pedido where pedido_id = ?")) {

            stmt.setInt(1, idPedido);
            var rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();

            return rs.getString(1);
        }
    }

    /**
     * Deleta registros criados em testes quando os mesmos possam interferir em outros testes
     */
    private void delete(int idPedido) throws SQLException {
        try (var conn = databaseConnection.jdbcConnection();
             var stmt1 = conn.prepareStatement("delete from pedido_item where pedido_id = ?");
             var stmt2 = conn.prepareStatement("delete from pedido where pedido_id = ?")) {

            stmt1.setInt(1, idPedido);
            stmt1.executeUpdate();

            stmt2.setInt(1, idPedido);
            stmt2.executeUpdate();
        }
    }

    private List<ItemPedido> sampleItens = List.of(
            new ItemPedido(1, new ItemCardapio(1, TipoItemCardapio.LANCHE, "Hamburger Vegetariano",
                    "Hamburger de ervilha com queijo prato", new ValorMonetario("22.90"))
            )
    );
}