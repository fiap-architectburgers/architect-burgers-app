package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdCliente;
import com.example.gomesrodris.archburgers.testUtils.RealDatabaseTestHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database Integration Tests
 */
class ClienteRepositoryJdbcImplIT {
    private static RealDatabaseTestHelper realDatabase;
    private DatabaseConnection databaseConnection;

    private ClienteRepositoryJdbcImpl repository;

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
        repository = new ClienteRepositoryJdbcImpl(databaseConnection);
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
    }

    @Test
    void getClienteByCpf() {
        var cliente = repository.getClienteByCpf(new Cpf("12332112340"));

        assertThat(cliente).isEqualTo(new Cliente(new IdCliente(1), "Roberto Carlos",
                new Cpf("12332112340"), "roberto.carlos@example.com"));
    }

    @Test
    void listarTodosClientes() {
        var clientes = repository.listarTodosClientes();

        assertThat(clientes).containsAll(List.of(
                new Cliente(new IdCliente(1), "Roberto Carlos",
                        new Cpf("12332112340"), "roberto.carlos@example.com"),
                new Cliente(new IdCliente(2), "Wanderleia",
                        new Cpf("99988877714"), "wanderleia@example.com")
        ));
    }

    @Test
    void getClienteById() {
        var cliente = repository.getClienteById(2);

        assertThat(cliente).isEqualTo(new Cliente(new IdCliente(2), "Wanderleia",
                new Cpf("99988877714"), "wanderleia@example.com"));
    }

    @Test
    void salvarCliente() {
        var cliente = new Cliente(null, "Erasmo",
                new Cpf("33344455508"), "erasmo@example.com");

        var clienteSalvo = repository.salvarCliente(cliente);

        assertThat(clienteSalvo).isEqualTo(new Cliente(new IdCliente(3), "Erasmo",
                new Cpf("33344455508"), "erasmo@example.com"));
    }

    @Test
    void getClienteByCpf_notFound() {
        var cliente = repository.getClienteByCpf(new Cpf("11122233396"));
        assertThat(cliente).isNull();
    }
}