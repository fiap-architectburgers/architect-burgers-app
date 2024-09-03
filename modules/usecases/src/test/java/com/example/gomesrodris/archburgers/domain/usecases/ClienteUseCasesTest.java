package com.example.gomesrodris.archburgers.domain.usecases;//import static org.junit.jupiter.api.Assertions.*;

import com.example.gomesrodris.archburgers.domain.auth.GrupoUsuario;
import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import com.example.gomesrodris.archburgers.domain.datagateway.ClienteGateway;
import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.exception.DomainPermissionException;
import com.example.gomesrodris.archburgers.domain.external.ProvedorAutenticacaoExterno;
import com.example.gomesrodris.archburgers.domain.usecaseparam.CadastrarClienteParam;
import com.example.gomesrodris.archburgers.domain.valueobjects.Cpf;
import com.example.gomesrodris.archburgers.domain.valueobjects.IdCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClienteUseCasesTest {
    private ClienteGateway clienteGateway;
    private ProvedorAutenticacaoExterno provedorAutenticacaoExterno;

    private ClienteUseCases clienteUseCases;

    @BeforeEach
    void setUp() {
        clienteGateway = mock(ClienteGateway.class);
        provedorAutenticacaoExterno = mock(ProvedorAutenticacaoExterno.class);

        clienteUseCases = new ClienteUseCases(clienteGateway, provedorAutenticacaoExterno);
    }

    @Test
    void getClienteByCredencial_naoLogado() {
        var cred = mock(UsuarioLogado.class);
        when(cred.autenticado()).thenReturn(false);
        when(cred.authError()).thenReturn("Não logado");

        assertThat(
                assertThrows(DomainPermissionException.class,
                        () -> clienteUseCases.getClienteByCredencial(cred))
        ).hasMessage("Usuário logado inválido. Não logado");
    }

    @Test
    void getClienteByCredencial_grupoIncorreto() {
        var cred = mock(UsuarioLogado.class);
        when(cred.autenticado()).thenReturn(true);
        when(cred.getGrupo()).thenReturn(GrupoUsuario.ClienteAnonimo);

        assertThat(
                assertThrows(DomainPermissionException.class,
                        () -> clienteUseCases.getClienteByCredencial(cred))
        ).hasMessage("Usuário logado inválido. Não pertence ao grupo ClienteCadastrado");
    }

    @Test
    void getClienteByCredencial_ok() throws DomainPermissionException {
        var cred = mock(UsuarioLogado.class);
        when(cred.autenticado()).thenReturn(true);
        when(cred.getGrupo()).thenReturn(GrupoUsuario.ClienteCadastrado);
        when(cred.getCpf()).thenReturn("12332112340");

        when(clienteGateway.getClienteByCpf(new Cpf("12332112340"))).thenReturn(new Cliente(
                new IdCliente(15), "Dudu Hamburger", new Cpf("12332112340"), "dudu@example.com"
        ));

        var result = clienteUseCases.getClienteByCredencial(cred);

        assertThat(result).isEqualTo(new Cliente(
                new IdCliente(15), "Dudu Hamburger", new Cpf("12332112340"), "dudu@example.com"
        ));
    }

    @Test
    void cadastrarCliente() {
        var expectedSalvo = new Cliente(
                new IdCliente(16), "Dudu Hamburger", new Cpf("12332112340"), "dudu@example.com"
        );

        when(clienteGateway.salvarCliente(new Cliente(
                null, "Dudu Hamburger", new Cpf("12332112340"), "dudu@example.com"
        ))).thenReturn(expectedSalvo);

        var result = clienteUseCases.cadastrarCliente(new CadastrarClienteParam(
                "Dudu Hamburger", "12332112340", "dudu@example.com", "secretBurger"));

        assertThat(result).isEqualTo(expectedSalvo);

        verify(provedorAutenticacaoExterno).registrarCliente(expectedSalvo, "secretBurger");
    }
}