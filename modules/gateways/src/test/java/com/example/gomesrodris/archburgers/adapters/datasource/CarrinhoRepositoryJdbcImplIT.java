package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.entities.ItemCardapio;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdCliente;
import com.example.gomesrodris.archburgers.domain.valueobjects.TipoItemCardapio;
import com.example.gomesrodris.archburgers.domain.valueobjects.ValorMonetario;
import com.example.gomesrodris.archburgers.testUtils.RealDatabaseTestHelper;
import org.junit.jupiter.api.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database Integration Tests
 */
class CarrinhoRepositoryJdbcImplIT {
    private static RealDatabaseTestHelper realDatabase;
    private DatabaseConnection databaseConnection;

    private CarrinhoRepositoryJdbcImpl carrinhoRepository;

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
        carrinhoRepository = new CarrinhoRepositoryJdbcImpl(databaseConnection);
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
    }

    @Test
    void getCarrinhoSalvoByCliente_naoExiste() {
        var carrinhoSalvo = carrinhoRepository.getCarrinhoSalvoByCliente(new IdCliente(77));
        assertThat(carrinhoSalvo).isNull();
    }

    @Test
    void getCarrinhoSalvoByCliente() {
        var carrinhoSalvo = carrinhoRepository.getCarrinhoSalvoByCliente(new IdCliente(2));
        assertThat(carrinhoSalvo).isNotNull();
        assertThat(carrinhoSalvo.id()).isEqualTo(1);

        assertThat(carrinhoSalvo.idClienteIdentificado()).isEqualTo(new IdCliente(2));
        assertThat(carrinhoSalvo.nomeClienteNaoIdentificado()).isNull();

        assertThat(carrinhoSalvo.observacoes()).isEqualTo("Sem cebola");
        assertThat(carrinhoSalvo.dataHoraCarrinhoCriado()).isEqualTo(LocalDateTime.of(2024, 4, 30, 15, 32, 58));
    }

    @Test
    void getCarrinhoSalvoById() {
        var carrinhoSalvo = carrinhoRepository.getCarrinho(1);
        assertThat(carrinhoSalvo).isNotNull();
        assertThat(carrinhoSalvo.id()).isEqualTo(1);

        assertThat(carrinhoSalvo.idClienteIdentificado()).isEqualTo(new IdCliente(2));
        assertThat(carrinhoSalvo.nomeClienteNaoIdentificado()).isNull();

        assertThat(carrinhoSalvo.observacoes()).isEqualTo("Sem cebola");
        assertThat(carrinhoSalvo.dataHoraCarrinhoCriado()).isEqualTo(LocalDateTime.of(2024, 4, 30, 15, 32, 58));
    }

    @Test
    void salvarCarrinhoVazio_clienteIdentificado() {
        Carrinho carrinho = Carrinho.newCarrinhoVazioClienteIdentificado(new IdCliente(1),
                LocalDateTime.of(2024, 4, 30, 20, 21, 40));

        var result = carrinhoRepository.salvarCarrinhoVazio(carrinho);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();

        var carrinhoSalvo = carrinhoRepository.getCarrinho(result.id());
        assertThat(carrinhoSalvo.idClienteIdentificado()).isEqualTo(new IdCliente(1));
        assertThat(carrinhoSalvo.dataHoraCarrinhoCriado()).isEqualTo(
                LocalDateTime.of(2024, 4, 30, 20, 21, 40)
        );
    }

    @Test
    void salvarCarrinhoVazio_clienteNaoIdentificado() {
        Carrinho carrinho = Carrinho.newCarrinhoVazioClienteNaoIdentificado("Sr. Anônimo",
                LocalDateTime.of(2024, 4, 30, 20, 21, 40));

        var result = carrinhoRepository.salvarCarrinhoVazio(carrinho);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();

        var carrinhoSalvo = carrinhoRepository.getCarrinho(result.id());
        assertThat(carrinhoSalvo.idClienteIdentificado()).isNull();
        assertThat(carrinhoSalvo.nomeClienteNaoIdentificado()).isEqualTo("Sr. Anônimo");
        assertThat(carrinhoSalvo.dataHoraCarrinhoCriado()).isEqualTo(
                LocalDateTime.of(2024, 4, 30, 20, 21, 40)
        );
    }

    @Test
    void salvarItemCarrinho() throws SQLException {
        var carrinhoSalvo = carrinhoRepository.getCarrinhoSalvoByCliente(new IdCliente(2));
        assertThat(carrinhoSalvo).isNotNull();

        carrinhoRepository.salvarItemCarrinho(carrinhoSalvo, new ItemPedido(3,
                        new ItemCardapio(7, TipoItemCardapio.SOBREMESA, "Mini churros",
                                "Mini churros de doce de leite", new ValorMonetario("0.99"))
                )
        );

        try (var conn = databaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("select * from carrinho_item where carrinho_id = 1 and item_cardapio_id = 7 and num_sequencia = 3");
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue(); // Record exists
        }
    }

    @Test
    void deleteCarrinho() throws SQLException {
        Carrinho carrinho = Carrinho.newCarrinhoVazioClienteNaoIdentificado("Sr. Anônimo",
                LocalDateTime.of(2024, 4, 30, 20, 21, 40));

        var savedCarrinho = carrinhoRepository.salvarCarrinhoVazio(carrinho);
        assertThat(savedCarrinho.id()).isGreaterThan(1);

        carrinhoRepository.salvarItemCarrinho(savedCarrinho, new ItemPedido(1,
                        new ItemCardapio(1, TipoItemCardapio.SOBREMESA, "Hamburger",
                                "Hamburger", new ValorMonetario("20.50"))
                )
        );

        var counts = getCounts(Objects.requireNonNull(savedCarrinho.id(), "Non null was expected"));
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);

        carrinhoRepository.deleteCarrinho(savedCarrinho);

        counts = getCounts(savedCarrinho.id());
        assertThat(counts[0]).isEqualTo(0);
        assertThat(counts[1]).isEqualTo(0);
    }

    @Test
    void updateObservacaoCarrinho() {
        var carrinho = carrinhoRepository.getCarrinho(1);
        assertThat(carrinho.observacoes()).isEqualTo("Sem cebola");

        carrinho = carrinho.setObservacoes("Sem pickles e sem muito sal na batata");
        carrinhoRepository.updateObservacaoCarrinho(carrinho);

        var newCarrinho = carrinhoRepository.getCarrinho(1);
        assertThat(newCarrinho.observacoes()).isEqualTo("Sem pickles e sem muito sal na batata");
    }

    private int[] getCounts(int idCarrinho) throws SQLException {
        int[] counts = new int[2];
        try (var conn = databaseConnection.jdbcConnection();
             var stmt1 = conn.prepareStatement("select count(*) from carrinho where carrinho_id = ?");
             var stmt2 = conn.prepareStatement("select count(*) from carrinho_item where carrinho_id = ?")) {

            stmt1.setInt(1, idCarrinho);
            var rs1 = stmt1.executeQuery();
            stmt2.setInt(1, idCarrinho);
            var rs2 = stmt2.executeQuery();

            assertThat(rs1.next()).isTrue();
            assertThat(rs2.next()).isTrue();

            counts[0] = rs1.getInt(1);
            counts[1] = rs2.getInt(1);
        }
        return counts;
    }


}