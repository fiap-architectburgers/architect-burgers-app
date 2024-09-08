package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdFormaPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.ValorMonetario;
import com.example.gomesrodris.archburgers.testUtils.RealDatabaseTestHelper;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database Integration Tests
 */
class PagamentoRepositoryJdbcImplIT {
    private static RealDatabaseTestHelper realDatabase;
    private DatabaseConnection databaseConnection;

    private PagamentoRepositoryJdbcImpl repository;

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
        repository = new PagamentoRepositoryJdbcImpl(databaseConnection);
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
    }

    @Test
    void salvarPagamento() throws SQLException {
        var saved = repository.salvarPagamento(new Pagamento(null, 2, IdFormaPagamento.CARTAO_MAQUINA,
                StatusPagamento.PENDENTE, new ValorMonetario("19.78"),
                LocalDateTime.of(2024, 6, 19, 21, 1, 50),
                LocalDateTime.of(2024, 6, 19, 22, 2, 52),
                "abc-def-ghi-jkl", "orderXy"));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.id()).isEqualTo(3);

        try (var conn = databaseConnection.jdbcConnection();
             var stmt = conn.prepareStatement("select id_pedido,forma_pagamento,valor," +
                     "status,data_hora_criacao,data_hora_atualizacao,codigo_pagamento_cliente,id_pedido_sistema_externo " +
                     "from pagamento where pagamento_id = ?")) {

            stmt.setInt(1, saved.id());
            var rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();

            assertThat(rs.getInt(1)).isEqualTo(2);
            assertThat(rs.getString(2)).isEqualTo("CARTAO_MAQUINA");
            assertThat(rs.getObject(3, BigDecimal.class)).isEqualTo(new BigDecimal("19.78"));
            assertThat(rs.getString(4)).isEqualTo("PENDENTE");
            assertThat(rs.getObject(5, LocalDateTime.class)).isEqualTo(
                    LocalDateTime.of(2024, 6, 19, 21, 1, 50));
            assertThat(rs.getObject(6, LocalDateTime.class)).isEqualTo(
                    LocalDateTime.of(2024, 6, 19, 22, 2, 52));
            assertThat(rs.getString(7)).isEqualTo("abc-def-ghi-jkl");
            assertThat(rs.getString(8)).isEqualTo("orderXy");
        }
    }

    @Test
    void findPagamentoByPedido() {
        var pagamento = repository.findPagamentoByPedido(1);

        assertThat(pagamento).isNotNull();

        assertThat(pagamento.idPedido()).isEqualTo(1);
        assertThat(pagamento.formaPagamento()).isEqualTo(new IdFormaPagamento("Integracao_GatewayX"));
        assertThat(pagamento.valor()).isEqualTo(new ValorMonetario("28.90"));
        assertThat(pagamento.status()).isEqualTo(StatusPagamento.FINALIZADO);
        assertThat(pagamento.dataHoraCriacao()).isEqualTo(LocalDateTime.of(2024,5,18,15,30,12));
        assertThat(pagamento.dataHoraAtualizacao()).isEqualTo(LocalDateTime.of(2024,5,18,15,31,12));
        assertThat(pagamento.codigoPagamentoCliente()).isEqualTo("barcode_data:1234567890");
        assertThat(pagamento.idPedidoSistemaExterno()).isEqualTo("2024-9988");
    }

    @Test
    void updateStatus() {
        var pagamento = repository.findPagamentoByPedido(5);

        assertThat(pagamento.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(pagamento.dataHoraCriacao()).isEqualTo(LocalDateTime.of(2024,5,17,14,30,12));
        assertThat(pagamento.dataHoraAtualizacao()).isEqualTo(LocalDateTime.of(2024,5,17,14,30,12));
        assertThat(pagamento.idPedidoSistemaExterno()).isEqualTo("98-7654-321");

        var finalizadoSalvar = pagamento.finalizar(
                LocalDateTime.of(2024,5,17,14,31,42), "abc-def-ghi");

        repository.updateStatus(finalizadoSalvar);

        var aposUpdate = repository.findPagamentoByPedido(5);

        assertThat(aposUpdate.status()).isEqualTo(StatusPagamento.FINALIZADO);
        assertThat(aposUpdate.dataHoraCriacao()).isEqualTo(LocalDateTime.of(2024,5,17,14,30,12));
        assertThat(aposUpdate.dataHoraAtualizacao()).isEqualTo(LocalDateTime.of(2024,5,17,14,31,42));
        assertThat(aposUpdate.idPedidoSistemaExterno()).isEqualTo("abc-def-ghi");
    }
}