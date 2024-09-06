package com.example.gomesrodris.archburgers.domain.auth;

/**
 * Representação de um usuário autenticado no sistema.
 * É responsabilidade das camadas mais externas interagir com o mecanismo de autenticação e autorização, fornecendo
 * instâncias válidas para a camada de use cases
 */
public interface UsuarioLogado {
    boolean autenticado();

    String getNome();
    String getCpf();
    String getEmail();

    GrupoUsuario getGrupo();

    String identityToken();

    String authError();
}
