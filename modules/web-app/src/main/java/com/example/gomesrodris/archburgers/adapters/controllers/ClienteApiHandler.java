package com.example.gomesrodris.archburgers.adapters.controllers;

import com.example.gomesrodris.archburgers.adapters.auth.UsuarioLogadoTokenParser;
import com.example.gomesrodris.archburgers.adapters.datasource.TransactionManager;
import com.example.gomesrodris.archburgers.adapters.dto.ClienteDto;
import com.example.gomesrodris.archburgers.adapters.dto.ClienteWithTokenDto;
import com.example.gomesrodris.archburgers.apiutils.WebUtils;
import com.example.gomesrodris.archburgers.controller.ClienteController;
import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.exception.DomainPermissionException;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CadastrarClienteParam;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ClienteApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClienteApiHandler.class);

    private final ClienteController clienteController;
    private final UsuarioLogadoTokenParser usuarioLogadoTokenParser;
    private final TransactionManager transactionManager;

    @Autowired
    public ClienteApiHandler(ClienteController clienteController,
                             UsuarioLogadoTokenParser usuarioLogadoTokenParser,
                             TransactionManager transactionManager) {
        this.clienteController = clienteController;
        this.usuarioLogadoTokenParser = usuarioLogadoTokenParser;
        this.transactionManager = transactionManager;
    }

    @Operation(description = "Busca dados do cliente a partir dos dados de autenticação, para identificação no início da compra")
    @GetMapping(path = "/cliente/conectado")
    public ResponseEntity<ClienteWithTokenDto> getClienteConectado(@RequestHeader HttpHeaders headers) {
        try {
            UsuarioLogado usuarioLogado = usuarioLogadoTokenParser.verificarUsuarioLogado(headers);

            return WebUtils.okResponse(ClienteWithTokenDto.fromEntity(
                    clienteController.getClienteByCredencial(usuarioLogado), usuarioLogado.identityToken()));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (DomainPermissionException dpe) {
            return WebUtils.errorResponse(HttpStatus.UNAUTHORIZED, dpe.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao recuperar cliente: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao recuperar cliente");
        }
    }

    @Operation(description = "Lista todos os clientes")
    @GetMapping(path = "/clientes")
    public ResponseEntity<List<ClienteDto>> getClientes() {
        var clientes = clienteController.listTodosClientes();
        return WebUtils.okResponse(clientes.stream().map(ClienteDto::fromEntity).toList());
    }

    @Operation(description = "Cadastra um novo cliente")
    @PostMapping(path = "/clientes")
    public ResponseEntity<ClienteDto> novoCliente(@RequestBody Map<String, String> paramMap) {
        try {
            var param = CadastrarClienteParam.fromMap(paramMap);
            var newCliente = transactionManager.runInTransaction(() -> clienteController.cadastrarCliente(param));
            return WebUtils.okResponse(ClienteDto.fromEntity(newCliente));
        } catch (IllegalArgumentException iae) {
            return WebUtils.errorResponse(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao cadastrar cliente: {}", e, e);
            return WebUtils.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro ao cadastrar cliente");
        }
    }
}
