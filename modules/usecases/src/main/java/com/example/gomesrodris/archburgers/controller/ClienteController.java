package com.example.gomesrodris.archburgers.controller;

import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.datagateway.ClienteGateway;
import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.exception.DomainPermissionException;
import com.example.gomesrodris.archburgers.domain.external.ProvedorAutenticacaoExterno;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CadastrarClienteParam;
import com.example.gomesrodris.archburgers.domain.usecases.ClienteUseCases;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;

import java.util.List;

public class ClienteController {
    private final ClienteUseCases clienteUseCases;

    public ClienteController(ClienteGateway clienteGateway, ProvedorAutenticacaoExterno provedorAutenticacaoExterno) {
        clienteUseCases = new ClienteUseCases(clienteGateway, provedorAutenticacaoExterno);
    }

    public Cliente getClienteByCredencial(UsuarioLogado usuarioLogado)  throws DomainPermissionException {
        return clienteUseCases.getClienteByCredencial(usuarioLogado);
    }

    public List<Cliente> listTodosClientes() {
        return clienteUseCases.listTodosClientes();
    }

    public Cliente cadastrarCliente(CadastrarClienteParam param) {
        return clienteUseCases.cadastrarCliente(param);
    }
}
