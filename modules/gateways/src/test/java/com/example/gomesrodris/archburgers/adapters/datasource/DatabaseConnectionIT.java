package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.testUtils.RealDatabaseTestHelper;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Database Integration Tests
 */
class DatabaseConnectionIT {
    private static RealDatabaseTestHelper realDatabase;

    private DatabaseConnection databaseConnection;

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
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
    }

    @Test
    void runWithoutTransaction_autoCommit() throws Exception {
        assertThat(databaseConnection.isInTransaction()).isFalse();

        DatabaseConnection.ConnectionInstance conn = databaseConnection.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement("update cliente set nome = 'Novo Nome XYZ' where cliente_id = ?");
            stmt.setInt(1, 2);
            stmt.execute();
            stmt.close();
        } finally {
            assertThat(conn.isClosed()).isFalse();
            conn.close();
        }

        assertThat(conn.isClosed()).isTrue();

        assertThat(databaseConnection.isInTransaction()).isFalse();

        // Check in an exclusive new connection
        AtomicReference<String> newValue = new AtomicReference<>();

        try (Connection directConnection = DriverManager.getConnection(
                realDatabase.getJdbcUrl(), realDatabase.getJdbcUsername(), realDatabase.getJdbcPassword())) {
            PreparedStatement checkStmt = directConnection.prepareStatement("select nome from cliente where cliente_id = ?");
            checkStmt.setInt(1, 2);
            var rs = checkStmt.executeQuery();
            assertThat(rs.next()).isTrue();
            newValue.set(rs.getString(1));
            rs.close();
            checkStmt.close();
        }

        assertThat(newValue.get()).isEqualTo("Novo Nome XYZ");
    }

    @Test
    void runInTransaction_check() throws Exception {
        assertThat(databaseConnection.isInTransaction()).isFalse();

        AtomicBoolean transactionFlag = new AtomicBoolean(false);
        databaseConnection.runInTransaction(() -> {
            transactionFlag.set(databaseConnection.isInTransaction());
            return null;
        });

        assertThat(transactionFlag.get()).isTrue();
        assertThat(databaseConnection.isInTransaction()).isFalse();
    }

    @Test
    void runInTransaction_commit() throws Exception {
        /*
        Testando todos os comportamentos esperados de uma sequência de instruções bem sucedidas numa Transação...
         */
        AtomicReference<DatabaseConnection.ConnectionInstance> conn1 = new AtomicReference<>();
        AtomicReference<DatabaseConnection.ConnectionInstance> conn2 = new AtomicReference<>();

        // Iniciando a transação. dummyReturn irá conter o valor retornado pelo lambda
        String dummyReturn = databaseConnection.runInTransaction(() -> {
            // 1. Obtém conexão e realiza uma mudança
            DatabaseConnection.ConnectionInstance conn = databaseConnection.getConnection();
            try {
                PreparedStatement stmt = conn.prepareStatement("update cliente set nome = 'Novo Nome XYZ' where cliente_id = ?");
                stmt.setInt(1, 2);
                stmt.execute();
                stmt.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                conn.close();
            }

            // 2. Verifica que o close é um no-op quando estamos na transação. Ela será fechada somente no final
            assertThat(conn.isClosed()).isFalse();
            conn1.set(conn);

            // 3. Em uma "nova" conexão, realiza uma segunda mudança
            conn = databaseConnection.getConnection();
            try {
                PreparedStatement stmt2 = conn.prepareStatement("update item_cardapio set nome = 'Novo Item AbC' where item_cardapio_id = ?");
                stmt2.setInt(1, 7);
                stmt2.execute();
                stmt2.close();

                // 4. Terminando a transação com uma query de leitura que é seu retorno
                PreparedStatement returnStmt = conn.prepareStatement("select '999' as value");
                var rs = returnStmt.executeQuery();
                assertThat(rs.next()).isTrue();
                var result = rs.getString(1);
                rs.close();
                returnStmt.close();

                assertThat(conn.isClosed()).isFalse();
                conn2.set(conn);

                return result;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        // 5. Transação finalizada com sucesso

        // 6. Verifica retorno
        assertThat(dummyReturn).isEqualTo("999");
        // 7. Verifica que as 2 referências de conexão obtidas anteriormente correspondem à mesma,
        //    e que agora ela foi fechada
        assertThat(conn1.get()).isSameAs(conn2.get());
        assertThat(conn1.get().isClosed()).isTrue();

        AtomicReference<String> newValue1 = new AtomicReference<>();
        AtomicReference<String> newValue2 = new AtomicReference<>();

        // 8. Obtém uma nova e exclusiva conexão direta para checar se as alterações estão persistidas
        //    Consulta ambos os valores modificados na transação anterior, comparando com o que foi definido
        try (Connection directConnection = DriverManager.getConnection(
                realDatabase.getJdbcUrl(), realDatabase.getJdbcUsername(), realDatabase.getJdbcPassword())) {
            {
                PreparedStatement checkStmt = directConnection.prepareStatement("select nome from cliente where cliente_id = ?");
                checkStmt.setInt(1, 2);
                var rs = checkStmt.executeQuery();
                assertThat(rs.next()).isTrue();
                newValue1.set(rs.getString(1));
                rs.close();
                checkStmt.close();
            }

            PreparedStatement checkStmt2 = directConnection.prepareStatement("select nome from item_cardapio where item_cardapio_id = ?");
            checkStmt2.setInt(1, 7);
            var rs2 = checkStmt2.executeQuery();
            assertThat(rs2.next()).isTrue();
            newValue2.set(rs2.getString(1));
            rs2.close();
            checkStmt2.close();
        }

        assertThat(newValue1.get()).isEqualTo("Novo Nome XYZ");
        assertThat(newValue2.get()).isEqualTo("Novo Item AbC");
    }


    @Test
    void runInTransaction_rollback() throws Exception {
        /*
        Testando todos os comportamentos esperados em uma transação que sofreu rollback após uma falha
         */
        AtomicReference<String> valueInTransaction = new AtomicReference<>();

        try {
            databaseConnection.runInTransaction(() -> {
                // 1. Realiza update dentro da transação
                DatabaseConnection.ConnectionInstance conn = databaseConnection.getConnection();
                try {
                    PreparedStatement stmt = conn.prepareStatement("update cliente set nome = 'Novo Nome XYZ v2' where cliente_id = ?");
                    stmt.setInt(1, 1);
                    stmt.execute();
                    stmt.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } finally {
                    conn.close();
                }

                // 2. Ainda na mesma transação obtém o resultado do update
                conn = databaseConnection.getConnection();
                try {
                    PreparedStatement checkStmt = conn.prepareStatement("select nome from cliente where cliente_id = ?");
                    checkStmt.setInt(1, 1);
                    var rs = checkStmt.executeQuery();
                    assertThat(rs.next()).isTrue();
                    valueInTransaction.set(rs.getString(1));
                    rs.close();
                    checkStmt.close();

                    // 3. Agora introduz um comando com Erro, que causará o rollback
                    PreparedStatement returnStmt = conn.prepareStatement("select noNoNo from cliente");
                    returnStmt.executeQuery();

                    return null;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            fail("Exception was expected in the previous instruction");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("does not exist");
        }

        assertThat(valueInTransaction.get()).isEqualTo("Novo Nome XYZ v2");

        // 4. Verifica que a alteração feita no primeiro update sofreu rollback
        AtomicReference<String> newValue1 = new AtomicReference<>();

        try (Connection directConnection = DriverManager.getConnection(
                realDatabase.getJdbcUrl(), realDatabase.getJdbcUsername(), realDatabase.getJdbcPassword())) {
            {
                PreparedStatement checkStmt = directConnection.prepareStatement("select nome from cliente where cliente_id = ?");
                checkStmt.setInt(1, 1);
                var rs = checkStmt.executeQuery();
                assertThat(rs.next()).isTrue();
                newValue1.set(rs.getString(1));
                rs.close();
                checkStmt.close();
            }
        }

        assertThat(newValue1.get()).isEqualTo("Roberto Carlos"); // Revertido para valor original da Migration
    }
}