package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.datasource.PedidoDataSource;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdCliente;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdFormaPagamento;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPedido;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the Repository based on a relational database via JDBC
 */
@Repository
public class PedidoRepositoryJdbcImpl implements PedidoDataSource {
    @Language("SQL")
    private static final String SQL_SELECT_PEDIDO_BY_ID = """
            select pedido_id,id_cliente_identificado,nome_cliente_nao_identificado,
               observacoes,status,forma_pagamento,data_hora_pedido
            from pedido where pedido_id = ?
            """;

    /*
     *  Regras de ordenação:
     *  Pronto > Em Preparação > Recebido > Em Pagamento
     *  Mais antigos primeiro
     */
    @Language("SQL")
    private static final String SQL_SELECT_PEDIDOS_BY_STATUS = """
            select pedido_id,id_cliente_identificado,nome_cliente_nao_identificado,
               observacoes,status,forma_pagamento,data_hora_pedido
            from pedido where status IN (/*PARAM_PLACEHOLDERS*/)
                        /*FILTRO_DATA_HORA_OPCIONAL*/
            order by
                case WHEN status = 'PRONTO' then 0
                     WHEN status = 'PREPARACAO' then 1
                     WHEN status = 'RECEBIDO' then 2
                     WHEN status = 'PAGAMENTO' then 3
                     WHEN status = 'FINALIZADO' then 4
                else 5 END asc,
                data_hora_pedido asc
            """;

    @Language("SQL")
    private static final String SQL_INSERT_PEDIDO = """
            insert into pedido (id_cliente_identificado,nome_cliente_nao_identificado,
               observacoes,status,forma_pagamento,data_hora_pedido)
            values (?,?,?,?,?,?) returning pedido_id;
            """;

    @Language("SQL")
    private static final String SQL_DELETE_PEDIDO = """
            DELETE from pedido where pedido_id = ?;
            """;

    @Language("SQL")
    private static final String SQL_INSERT_ITEM = """
            insert into pedido_item (pedido_id, item_cardapio_id, num_sequencia)
            values (?,?,?);
            """;

    @Language("SQL")
    private static final String SQL_DELETE_ITEM = """
            DELETE from pedido_item where pedido_id = ?;
            """;

    @Language("SQL")
    private static final String SQL_UPDATE_STATUS = """
            update pedido set status = ? where pedido_id = ?;
            """;

    private final DatabaseConnection databaseConnection;

    public PedidoRepositoryJdbcImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public Pedido getPedido(int idPedido) {
        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_SELECT_PEDIDO_BY_ID)) {
            stmt.setInt(1, idPedido);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                return null;

            return pedidoFromResultSet(rs);
        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Pedido savePedido(Pedido pedido) {
        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_INSERT_PEDIDO);
             var stmtItem = connection.prepareStatement(SQL_INSERT_ITEM)) {

            if (pedido.idClienteIdentificado() != null) {
                stmt.setInt(1, pedido.idClienteIdentificado().id());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            stmt.setString(2, pedido.nomeClienteNaoIdentificado());
            stmt.setString(3, pedido.observacoes());
            stmt.setString(4, pedido.status().name());
            stmt.setString(5, pedido.formaPagamento().codigo());
            stmt.setObject(6, pedido.dataHoraPedido());

            var rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new IllegalStateException("Unexpected state, query should return");
            }

            var id = rs.getInt(1);

            for (ItemPedido item : pedido.itens()) {
                stmtItem.clearParameters();
                stmtItem.setInt(1, id);
                stmtItem.setInt(2, item.itemCardapio().id());
                stmtItem.setInt(3, item.numSequencia());
                stmtItem.executeUpdate();
            }

            return pedido.withId(id);

        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Pedido> listPedidos(List<StatusPedido> filtroStatus,
                                    @Nullable LocalDateTime olderThan) {
        var sql =  SQL_SELECT_PEDIDOS_BY_STATUS.replace("/*PARAM_PLACEHOLDERS*/",
                filtroStatus.stream().map(s -> "?").collect(Collectors.joining(",")));

        if (olderThan != null) {
            sql = sql.replace("/*FILTRO_DATA_HORA_OPCIONAL*/", " and data_hora_pedido <= ?");
        }

        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(sql)) {

            for (int i = 0; i < filtroStatus.size(); i++) {
                stmt.setString(i + 1, filtroStatus.get(i).name());
            }

            if (olderThan != null) {
                stmt.setObject(filtroStatus.size() + 1, olderThan);
            }

            ResultSet rs = stmt.executeQuery();
            List<Pedido> result = new ArrayList<>();

            while (rs.next()) {
                result.add(pedidoFromResultSet(rs));
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(Pedido pedido) {
        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_UPDATE_STATUS)) {

            stmt.setString(1, pedido.status().name());
            stmt.setInt(2, Objects.requireNonNull(pedido.id(), "Must have ID to update"));

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }

    private static @NotNull Pedido pedidoFromResultSet(ResultSet rs) throws SQLException {
        var idClienteIdentificado = rs.getInt("id_cliente_identificado");

        IdFormaPagamento formaPagamento;
        try {
            formaPagamento = new IdFormaPagamento(rs.getString("forma_pagamento"));
        } catch (SQLException e) {
            throw new RuntimeException("Registro inconsistente! formaPagamento=" + rs.getString("forma_pagamento"));
        }

        StatusPedido status = null;
        try {
            status = StatusPedido.valueOf(rs.getString("status"));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Registro inconsistente! status=" + rs.getString("status"));
        }

        return Pedido.pedidoRecuperado(
                rs.getInt("pedido_id"),
                idClienteIdentificado > 0 ? new IdCliente(idClienteIdentificado) : null,
                rs.getString("nome_cliente_nao_identificado"),
                Collections.emptyList(),
                rs.getString("observacoes"),
                status,
                formaPagamento,
                rs.getTimestamp("data_hora_pedido").toLocalDateTime()
        );
    }

    @Override
    public void deletePedido(Integer idPedido) {
        try (var connection = databaseConnection.getConnection();
             var stmt = connection.prepareStatement(SQL_DELETE_PEDIDO);
             var stmtItem = connection.prepareStatement(SQL_DELETE_ITEM)) {

            if ((idPedido != null) && (idPedido > 0)) {
                stmt.setInt(1, idPedido);
                stmtItem.setInt(1, idPedido);
            } else {
                throw new IllegalStateException("Unexpected idPedido, query should return");
            }
            stmtItem.executeUpdate();
            stmt.executeUpdate();


        } catch (SQLException e) {
            throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
        }
    }
}
