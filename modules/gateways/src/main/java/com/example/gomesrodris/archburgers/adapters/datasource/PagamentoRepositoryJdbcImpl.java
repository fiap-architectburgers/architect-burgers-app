package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.datasource.PagamentoDataSource;
import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdFormaPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.ValorMonetario;
import org.intellij.lang.annotations.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Implementation of the Repository based on a relational database via JDBC
 */
@Repository
public class PagamentoRepositoryJdbcImpl implements PagamentoDataSource {
    @Language("SQL")
    private static final String SQL_FIND_BY_PEDIDO = """
                select
                    pagamento_id, forma_pagamento,status,valor,
                            data_hora_criacao,data_hora_atualizacao,codigo_pagamento_cliente,id_pedido_sistema_externo
                from pagamento where id_pedido = ?
            """;

    @Language("SQL")
    private static final String SQL_INSERT = """
                insert into pagamento (id_pedido,forma_pagamento,valor,
                    status,data_hora_criacao,data_hora_atualizacao,codigo_pagamento_cliente,id_pedido_sistema_externo)
                values (?,?,?,?,?,?,?,?)
                returning pagamento_id
            """;

    @Language("SQL")
    private static final String SQL_UPDATE_STATUS = """
                update pagamento set status = ?, data_hora_atualizacao = ?, id_pedido_sistema_externo = ?
                     where pagamento_id = ?
            """;

    @Language("SQL")
    private static final String SQL_DELETE_BY_ID_PEDIDO = """
                delete from pagamento where id_pedido = ?
            """;

    private final DatabaseConnection databaseConnection;

    @Autowired
    public PagamentoRepositoryJdbcImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public Pagamento findPagamentoByPedido(Integer idPedido) {
        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_FIND_BY_PEDIDO)) {

            stmt.setInt(1, idPedido);

            var rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return new Pagamento(
                    rs.getInt("pagamento_id"),
                    idPedido,
                    new IdFormaPagamento(rs.getString("forma_pagamento")),
                    StatusPagamento.valueOf(rs.getString("status")),
                    new ValorMonetario(rs.getObject("valor", BigDecimal.class)),
                    rs.getObject("data_hora_criacao", LocalDateTime.class),
                    rs.getObject("data_hora_atualizacao", LocalDateTime.class),
                    rs.getString("codigo_pagamento_cliente"),
                    rs.getString("id_pedido_sistema_externo")
            );
        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Pagamento salvarPagamento(Pagamento pagamento) {
        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, pagamento.idPedido());
            stmt.setString(2, pagamento.formaPagamento().codigo());
            stmt.setObject(3, pagamento.valor().asBigDecimal());
            stmt.setString(4, pagamento.status().name());
            stmt.setObject(5, pagamento.dataHoraCriacao());
            stmt.setObject(6, pagamento.dataHoraAtualizacao());
            stmt.setString(7, pagamento.codigoPagamentoCliente());
            stmt.setString(8, pagamento.idPedidoSistemaExterno());

            var rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new IllegalStateException("Inconsistent state! Insert was supposed to return 1");
            }

            return pagamento.withId(rs.getInt(1));

        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(Pagamento pagamento) {
        String status = Objects.requireNonNull(pagamento.status(), "Status deve estar persistido antes do Update").name();
        int id = Objects.requireNonNull(pagamento.id(), "Pagamento deve estar persistido antes do Update");

        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_UPDATE_STATUS)) {

            stmt.setString(1, status);
            stmt.setObject(2, pagamento.dataHoraAtualizacao());
            stmt.setString(3, pagamento.idPedidoSistemaExterno());
            stmt.setInt(4, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePagamentoByIdPedido(Integer idPedido) {

        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_DELETE_BY_ID_PEDIDO)) {

            if ((idPedido != null) && (idPedido > 0)) {
                stmt.setInt(1, idPedido);
            } else {
                throw new IllegalStateException("Unexpected idPedido, query should return");
            }
            stmt.executeUpdate();


        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }

    }
}
