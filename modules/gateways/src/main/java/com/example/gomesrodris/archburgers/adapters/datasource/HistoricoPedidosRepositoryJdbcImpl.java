package com.example.gomesrodris.archburgers.adapters.datasource;

import com.example.gomesrodris.archburgers.domain.datasource.HistoricoPedidosDataSource;
import com.example.gomesrodris.archburgers.domain.entities.ItemCardapio;
import com.example.gomesrodris.archburgers.domain.entities.ItemPedido;
import com.example.gomesrodris.archburgers.domain.entities.Pagamento;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the Repository based on a non-relational database via JDBC
 */
@Repository
public class HistoricoPedidosRepositoryJdbcImpl implements HistoricoPedidosDataSource {

    private final MongoDatabaseConnection databaseConnection;

    @Autowired
    public HistoricoPedidosRepositoryJdbcImpl(MongoDatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public void salvarHistoricoPedido(List<Pedido> pedidosAntigos, List<Pagamento> pagamentosAntigos) {
        MongoClientSettings settings = this.databaseConnection.getMongoClientSettings();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {

                MongoDatabase database = mongoClient.getDatabase(this.databaseConnection.getDatabase());
                MongoCollection<Document> collection = database.getCollection("pedidos");

                // Convertendo objetos Pedido para documentos
                for (Pedido ped : pedidosAntigos) {
                    System.out.println(ped.toString());
                    Document doc = new Document();

                    doc.append("_id", new ObjectId())
                            .append("id", ped.id())
                            .append("idClienteIdentificado", ped.idClienteIdentificado() == null ? "null" : ped.idClienteIdentificado().id())
                            .append("nomeClienteNaoIdentificado", ped.nomeClienteNaoIdentificado())
                            .append("observacoes", ped.observacoes())
                            .append("status", ped.status())
                            .append("formaPagamento", ped.formaPagamento().codigo())
                            .append("dataHoraPedido", ped.dataHoraPedido().toString());

                    List<Document> listItens = new ArrayList<>();
                    for (ItemPedido item : ped.itens()) {
                        ItemCardapio itemCardapio = item.itemCardapio();
                        Document docItemPedido = new Document();

                        Document itemCardapioDoc = new Document("id", itemCardapio.id())
                                .append("tipo", itemCardapio.tipo())
                                .append("nome", itemCardapio.nome())
                                .append("descricao", itemCardapio.descricao())
                                .append("valor", itemCardapio.valor().toString());
                        docItemPedido.append("numSequencia", item.numSequencia())
                                .append("itemCardapio", itemCardapioDoc);
                        listItens.add(docItemPedido);
                    }
                    doc.append("itens", listItens);

                    Optional<Pagamento> pagamentoOptional = pagamentosAntigos.stream()
                            .filter(p -> p != null && p.idPedido() == ped.id()) // Check for null before calling idPedido()
                            .findFirst();

                    if (pagamentoOptional.isPresent()) {
                        Pagamento pagamento = pagamentoOptional.get();
                        Document docPagamento = new Document();

                        docPagamento.append("_id", new ObjectId())
                                .append("id", pagamento.id())
                                .append("formaPagamento", pagamento.formaPagamento().codigo())
                                .append("status", pagamento.status().toString())
                                .append("valor", pagamento.valor().toString())
                                .append("dataHoraCriacao", pagamento.dataHoraCriacao().toString())
                                .append("dataHoraAtualizacao", pagamento.dataHoraAtualizacao().toString())
                                .append("codigoPagamentoCliente", pagamento.codigoPagamentoCliente())
                                .append("idPedidoSistemaExterno", pagamento.idPedidoSistemaExterno());

                        doc.append("pagamento", docPagamento);
                    }

                    collection.insertOne(doc);
                }

                mongoClient.close();

            } catch (MongoException e) {
                throw new RuntimeException("(" + this.getClass().getSimpleName() + ") Database error: " + e.getMessage(), e);
            }
        }
    }

}
