package com.example.gomesrodris.archburgers.domain.services;

import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.repositories.CarrinhoRepository;
import com.example.gomesrodris.archburgers.domain.repositories.ClienteRepository;
import com.example.gomesrodris.archburgers.domain.support.TransactionManager;
import com.example.gomesrodris.archburgers.domain.utils.Clock;
import com.example.gomesrodris.archburgers.domain.utils.StringUtils;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarrinhoServices {

    private final CarrinhoRepository carrinhoRepository;
    private final ClienteRepository clienteRepository;
    private final TransactionManager transactionManager;
    private final Clock clock;

    private final RecuperarCarrinhoPolicy recuperarCarrinhoPolicy;
    private final SalvarClientePolicy salvarClientePolicy;

    @Autowired
    public CarrinhoServices(CarrinhoRepository carrinhoRepository, ClienteRepository clienteRepository,
                            TransactionManager transactionManager, Clock clock) {
        this.carrinhoRepository = carrinhoRepository;
        this.clienteRepository = clienteRepository;
        this.transactionManager = transactionManager;
        this.clock = clock;

        this.recuperarCarrinhoPolicy = new RecuperarCarrinhoPolicy();
        this.salvarClientePolicy = new SalvarClientePolicy();
    }

    public Carrinho criarCarrinho(@NotNull CarrinhoParam param) throws Exception {
        boolean clienteIdentificado = param.isClienteIdentificado();

        param.getCpfValidado(); // Throw early if invalid

        return transactionManager.runInTransaction(() -> {
            if (clienteIdentificado) {
                if (param.idCliente == null) {
                    // Sanity-check para atender warnings da IDE - ja verificado em param.getMetodo()
                    throw new RuntimeException("Unexpected state param.idCliente");
                }

                var carrinhoSalvo = recuperarCarrinhoPolicy.tryRecuperarCarrinho(param.idCliente);
                if (carrinhoSalvo != null) {
                    return carrinhoSalvo;
                }

                var cliente = clienteRepository.getClienteById(param.idCliente);
                if (cliente == null) {
                    throw new IllegalArgumentException("Cliente invalido! " + param.idCliente);
                }

                var newCarrinho = new Carrinho(null, cliente, null,
                        List.of(), null, clock.localDateTime());
                return carrinhoRepository.salvarCarrinho(newCarrinho);

            } else {
                Cliente novoCliente = salvarClientePolicy.salvarClienteSeDadosCompletos(param);
                var newCarrinho = new Carrinho(null, novoCliente,
                        novoCliente == null ? param.nomeCliente : null,
                        List.of(), null, clock.localDateTime());
                return carrinhoRepository.salvarCarrinho(newCarrinho);
            }
        });
    }

    /**
     * Parâmetros para criação de carrinho. Oferece tres possíveis combinações de atributos:
     * <ul>
     *     <li>idCliente: Para associar o carrinho a um cliente cadastrado</li>
     *     <li>Apenas nomeCliente: Cliente não identificado, chamar pelo nome apenas para este pedido</li>
     *     <li>nomeCliente, cpf, email: Cadastra o cliente para próximos pedidos</li>
     * </ul>
     */
    public record CarrinhoParam(
            @Nullable Integer idCliente,

            @Nullable String nomeCliente,

            @Nullable String cpf,
            @Nullable String email
    ) {
        private boolean isClienteIdentificado() {
            if (idCliente != null && StringUtils.isEmpty(nomeCliente) && StringUtils.isEmpty(cpf) && StringUtils.isEmpty(email)) {
                return true;
            } else if (idCliente == null && StringUtils.isNotEmpty(nomeCliente)) {
                return false;
            } else {
                throw new IllegalArgumentException("Combinação de parâmetros inválidos. Usar {idCliente} " +
                        "ou {nomeCliente} ou {nomeCliente, cpf, email}");
            }
        }

        @Nullable private Cpf getCpfValidado() {
            if (StringUtils.isEmpty(cpf)) {
                return null;
            } else {
                return new Cpf(cpf);
            }
        }
    }

    /**
     * Política do fluxo de compra: Se o cliente se identificou e há um carrinho salvo,
     * carrega para permitir continuar a compra
     */
    private class RecuperarCarrinhoPolicy {
        Carrinho tryRecuperarCarrinho(int idCliente) {
            return carrinhoRepository.getCarrinhoByClienteId(idCliente);
        }
    }

    /**
     * Política do fluxo de compra: Se foram informados dados completos cadastrar o cliente
     */
    private class SalvarClientePolicy {
        Cliente salvarClienteSeDadosCompletos(CarrinhoParam param) {
            Cpf cpf = param.getCpfValidado();
            if (StringUtils.isNotEmpty(param.nomeCliente) && StringUtils.isNotEmpty(param.email) && cpf != null) {
                Cliente checkExistente = clienteRepository.getClienteByCpf(cpf);
                if (checkExistente != null) {
                    throw new IllegalArgumentException("Cliente com CPF " + param.cpf + " já cadastrado");
                }

                Cliente newCliente = new Cliente(null, param.nomeCliente, cpf, param.email);
                return clienteRepository.salvarCliente(newCliente);
            } else {
                return null;
            }
        }
    }
}
