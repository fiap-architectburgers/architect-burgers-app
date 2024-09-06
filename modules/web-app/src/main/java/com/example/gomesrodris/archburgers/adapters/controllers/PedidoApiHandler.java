package com.example.gomesrodris.archburgers.adapters.controllers;

import com.example.gomesrodris.archburgers.adapters.auth.UsuarioLogadoTokenParser;
import com.example.gomesrodris.archburgers.adapters.datasource.TransactionManager;
import com.example.gomesrodris.archburgers.adapters.dto.PedidoDto;
import com.example.gomesrodris.archburgers.adapters.presenters.PedidoPresenter;
import com.example.gomesrodris.archburgers.apiutils.WebUtils;
import com.example.gomesrodris.archburgers.controller.PedidoController;
import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.entities.Pedido;
import com.example.gomesrodris.archburgers.domain.exception.DomainPermissionException;
import com.example.gomesrodris.archburgers.domain.exception.DomainArgumentException;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CriarPedidoParam;
import com.example.gomesrodris.archburgers.domain.utils.StringUtils;
import com.example.gomesrodris.archburgers.domain.valueobjects.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PedidoApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PedidoApiHandler.class);

    private final PedidoController pedidoController;
    private final UsuarioLogadoTokenParser usuarioLogadoTokenParser;
    private final TransactionManager transactionManager;

    @Autowired
    public PedidoApiHandler(PedidoController pedidoController,
                            UsuarioLogadoTokenParser usuarioLogadoTokenParser,
                            TransactionManager transactionManager) {
        this.pedidoController = pedidoController;
        this.usuarioLogadoTokenParser = usuarioLogadoTokenParser;
        this.transactionManager = transactionManager;
    }

    @Operation(summary = "Cria um pedido a partir do carrinho informado",
            description = "O pedido recebe todos os itens do carrinho, e após a criação do pedido o carrinho é excluído")
    @PostMapping(path = "/pedidos")
    public ResponseEntity<PedidoDto> criarPedido(
            @RequestHeader HttpHeaders headers,
            @RequestBody CriarPedidoParam param) {

        Pedido pedido;
        try {
            UsuarioLogado usuarioLogado = usuarioLogadoTokenParser.verificarUsuarioLogado(headers);

            pedido = transactionManager.runInTransaction(() -> pedidoController.criarPedido(param, usuarioLogado));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DomainPermissionException dpe) {
            return WebUtils.errorResponse(HttpStatus.FORBIDDEN, dpe.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao criar pedido: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao criar pedido");
        }

        return WebUtils.okResponse(PedidoPresenter.entityToPresentationDto(pedido));
    }

    @Operation(summary = "Lista os pedidos conforme critério informado",
            description = "Se não for informado nenhum dos filtros será feita busca de todos os pedidos ativos (excluindo Finalizado e Cancelado) seguindo ordenação padrão",
            parameters = {
                    @Parameter(name = "status", description = "Filtra por um status de pedido específico"),
                    @Parameter(name = "atraso (boolean)", description = "Filtra pedidos em atraso (RECEBIDO ou EM PREPARAÇÃO criados há mais de 20 minutos)")
            })
    @GetMapping(path = "/pedidos")
    public ResponseEntity<List<PedidoDto>> listarPedidos(
            @RequestParam(value = "status", required = false) String filtroStatus,
            @RequestParam(value = "atraso", required = false) String filtroAtraso) {
        List<Pedido> result;
        try {
            StatusPedido parsedFiltroStatus = StringUtils.isEmpty(filtroStatus)
                    ? null : StatusPedido.valueOf(filtroStatus);

            boolean isFiltroAtraso = Boolean.parseBoolean(filtroAtraso);

            if (isFiltroAtraso) {
                result = pedidoController.listarPedidosComAtraso();
            } else if (parsedFiltroStatus != null) {
                result = pedidoController.listarPedidosByStatus(parsedFiltroStatus);
            } else {
                result = pedidoController.listarPedidosAtivos();
            }

        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao listar pedidos: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao listar pedidos");
        }

        return WebUtils.okResponse(result.stream().map(PedidoPresenter::entityToPresentationDto).toList());
    }

    @PostMapping(path = "/pedidos/{idPedido}/validar")
    public ResponseEntity<PedidoDto> validarPedido(@PathVariable("idPedido") Integer idPedido) {
        Pedido pedido;
        try {
            pedido = transactionManager.runInTransaction(() -> pedidoController.validarPedido(idPedido));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao atualizar pedido: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao atualizar pedido");
        }

        return WebUtils.okResponse(PedidoPresenter.entityToPresentationDto(pedido));
    }

    @PostMapping(path = "/pedidos/{idPedido}/cancelar")
    public ResponseEntity<PedidoDto> cancelarPedido(@PathVariable("idPedido") Integer idPedido) {
        Pedido pedido;
        try {
            pedido = transactionManager.runInTransaction(() -> pedidoController.cancelarPedido(idPedido));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao atualizar pedido: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao atualizar pedido");
        }

        return WebUtils.okResponse(PedidoPresenter.entityToPresentationDto(pedido));
    }

    @PostMapping(path = "/pedidos/{idPedido}/setPronto")
    public ResponseEntity<PedidoDto> setPedidoPronto(@PathVariable("idPedido") Integer idPedido) {
        Pedido pedido;
        try {
            pedido = transactionManager.runInTransaction(() -> pedidoController.setPronto(idPedido));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao atualizar pedido: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao atualizar pedido");
        }

        return WebUtils.okResponse(PedidoPresenter.entityToPresentationDto(pedido));
    }

    @PostMapping(path = "/pedidos/{idPedido}/finalizar")
    public ResponseEntity<PedidoDto> finalizarPedido(@PathVariable("idPedido") Integer idPedido) {
        Pedido pedido;
        try {
            pedido = transactionManager.runInTransaction(() -> pedidoController.finalizarPedido(idPedido));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao atualizar pedido: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao atualizar pedido");
        }

        return WebUtils.okResponse(PedidoPresenter.entityToPresentationDto(pedido));
    }

    @PostMapping(value = "/pedidos/historico/arquivarPedidos/{dias}")
    public ResponseEntity arquivarPedidos(@PathVariable("dias") Integer dias) {
        try {
            pedidoController.arquivarPedidos(dias);
            return ResponseEntity.ok().build();

        } catch (DomainArgumentException ae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, ae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao obter pedido: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao obter pedido");
        }
    }
}
