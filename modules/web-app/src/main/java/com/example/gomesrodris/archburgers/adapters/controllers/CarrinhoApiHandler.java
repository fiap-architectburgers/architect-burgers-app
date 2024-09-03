package com.example.gomesrodris.archburgers.adapters.controllers;

import com.example.gomesrodris.archburgers.adapters.auth.UsuarioLogadoTokenParser;
import com.example.gomesrodris.archburgers.adapters.datasource.TransactionManager;
import com.example.gomesrodris.archburgers.adapters.dto.AddItemCarrinhoDto;
import com.example.gomesrodris.archburgers.adapters.dto.CarrinhoDto;
import com.example.gomesrodris.archburgers.adapters.dto.CarrinhoObservacoesDto;
import com.example.gomesrodris.archburgers.adapters.presenters.CarrinhoPresenter;
import com.example.gomesrodris.archburgers.apiutils.WebUtils;
import com.example.gomesrodris.archburgers.controller.CarrinhoController;
import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.entities.Carrinho;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarCarrinhoParam;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Métodos para manipulação do carrinho de compras
 */
@RestController
public class CarrinhoApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarrinhoApiHandler.class);

    private final CarrinhoController carrinhoController;
    private final UsuarioLogadoTokenParser usuarioLogadoTokenParser;
    private final TransactionManager transactionManager;

    @Autowired
    public CarrinhoApiHandler(CarrinhoController carrinhoController,
                              UsuarioLogadoTokenParser usuarioLogadoTokenParser,
                              TransactionManager transactionManager) {
        this.carrinhoController = carrinhoController;
        this.usuarioLogadoTokenParser = usuarioLogadoTokenParser;
        this.transactionManager = transactionManager;
    }

    @Operation(summary = "Obtém dados do carrinho a partir de seu ID")
    @GetMapping(path = "/carrinho/{idCarrinho}")
    public ResponseEntity<CarrinhoDto> findCarrinho(@PathVariable("idCarrinho") Integer idCarrinho) {

        try {
            if (idCarrinho == null)
                throw new IllegalArgumentException("Path param idCarrinho missing");

            var carrinho = carrinhoController.findCarrinho(idCarrinho);
            return WebUtils.okResponse(CarrinhoPresenter.entityToPresentationDto(carrinho));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao consultar carrinho: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao consultar carrinho");
        }
    }

    @Operation(summary = "Inicia um novo carrinho de compra",
    description = """
     Oferece tres possíveis combinações de atributos na requisição:
      - idCliente: Para associar o carrinho a um cliente cadastrado
      - Apenas nomeCliente: Cliente não identificado, chamar pelo nome apenas para este pedido
      - nomeCliente, cpf, email: Cadastra um novo cliente e associa à compra atual
    """)
    @PostMapping(path = "/carrinho")
    public ResponseEntity<CarrinhoDto> iniciarCarrinho(
            @RequestHeader HttpHeaders headers,
            @RequestBody CriarCarrinhoParam param) {

        Carrinho carrinho;
        try {
            UsuarioLogado usuarioLogado = usuarioLogadoTokenParser.verificarUsuarioLogado(headers);

            carrinho = transactionManager.runInTransaction(() -> carrinhoController.criarCarrinho(param, usuarioLogado));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao criar/recuperar carrinho: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao criar/recuperar carrinho");
        }

        return WebUtils.okResponse(CarrinhoPresenter.entityToPresentationDto(carrinho));
    }

    @Operation(summary = "Adiciona um item ao carrinho")
    @PostMapping(path = "/carrinho/{idCarrinho}")
    public ResponseEntity<CarrinhoDto> addItemCarrinho(@PathVariable("idCarrinho") Integer idCarrinho, @RequestBody AddItemCarrinhoDto param) {

        Carrinho carrinho;
        try {
            if (idCarrinho == null)
                throw new IllegalArgumentException("Path param idCarrinho missing");
            if (param == null)
                throw new IllegalArgumentException("Request body missing");

            carrinho = transactionManager.runInTransaction(() -> carrinhoController.addItem(
                    idCarrinho, param.validarIdItemCardapio()));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao adicionar item no carrinho: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao adicionar item no carrinho");
        }

        return WebUtils.okResponse(CarrinhoPresenter.entityToPresentationDto(carrinho));
    }

    @Operation(summary = "Exclui um item do carrinho")
    @DeleteMapping(path = "/carrinho/{idCarrinho}/itens/{numSequencia}")
    public ResponseEntity<CarrinhoDto> deleteItemCarrinho(
            @PathVariable("idCarrinho") Integer idCarrinho,
            @PathVariable("numSequencia") Integer numSequencia) {

        Carrinho carrinho;
        try {
            if (idCarrinho == null)
                throw new IllegalArgumentException("Path param idCarrinho missing");
            if (numSequencia == null)
                throw new IllegalArgumentException("Path param numSequencia missing");

            carrinho = transactionManager.runInTransaction(() -> carrinhoController.deleteItem(
                    idCarrinho, numSequencia));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao excluir item do carrinho: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao excluir item do carrinho");
        }

        return WebUtils.okResponse(CarrinhoPresenter.entityToPresentationDto(carrinho));
    }

    @Operation(summary = "Atribui ou atualiza o campo de observações do pedido")
    @PutMapping(path = "/carrinho/{idCarrinho}/obs")
    public ResponseEntity<CarrinhoDto> atualizarObservacoes(@PathVariable("idCarrinho") Integer idCarrinho,
                                                            @RequestBody CarrinhoObservacoesDto param) {
        Carrinho carrinho;
        try {
            if (idCarrinho == null)
                throw new IllegalArgumentException("Path param idCarrinho missing");
            if (param == null)
                throw new IllegalArgumentException("Request body missing");

            carrinho = transactionManager.runInTransaction(() -> carrinhoController.setObservacoes(idCarrinho, param.observacoes()));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao atualizar observacoes: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao atualizar observacoes");
        }

        return WebUtils.okResponse(CarrinhoPresenter.entityToPresentationDto(carrinho));
    }
}
