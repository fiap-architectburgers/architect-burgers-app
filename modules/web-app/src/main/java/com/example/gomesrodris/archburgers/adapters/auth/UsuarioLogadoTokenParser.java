package com.example.gomesrodris.archburgers.adapters.auth;

import com.example.gomesrodris.archburgers.domain.auth.GrupoUsuario;
import com.example.gomesrodris.archburgers.domain.auth.UsuarioLogado;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SecurityException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Integração com o autenticador externo da aplicação.
 * Protocolo para identificação do usuário logado: é esperado que o Autenticador insira nos headers da requisição
 * o IdToken do Cognito User Pool, sob a chave "IdentityToken"
 */
@Service
public class UsuarioLogadoTokenParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioLogadoTokenParser.class);

    public static final String HEADER_NAME = "IdentityToken";
    private static final String HEADER_NAME_LOWER = "identitytoken";

    private final JwtParser jwtParser;

    @Autowired
    public UsuarioLogadoTokenParser(CognitoJwksApi cognitoJwksApi) {
        jwtParser = Jwts.parser()
                .keyLocator(new JwksKeyLocator(cognitoJwksApi))
                .build();
    }

    @VisibleForTesting
    UsuarioLogadoTokenParser(CognitoJwksApi cognitoJwksApi, Clock expirationCheckClock) {
        jwtParser = Jwts.parser()
                .clock(expirationCheckClock)
                .keyLocator(new JwksKeyLocator(cognitoJwksApi))
                .build();
    }

    @NotNull
    public UsuarioLogado verificarUsuarioLogado(HttpHeaders headers) {
        String identityToken = headers.getFirst(HEADER_NAME);
        if (identityToken == null)
            identityToken = headers.getFirst(HEADER_NAME_LOWER);

        if (identityToken == null || identityToken.isBlank()) {
            return new TokenBasedUsuarioLogado(false, null, null, null, null,
                    "IdentityToken is missing", null);
        }

        Jwt<?, ?> jwt;
        try {
            jwt = jwtParser.parse(identityToken);
        } catch (ExpiredJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            LOGGER.warn("Erro validando IdentityToken: {} -- {}", e, identityToken);
            return new TokenBasedUsuarioLogado(false, null, null, null, null,
                    "Erro ao validar IdentityToken: " + e.getMessage(), null);
        }

        Claims claims = (Claims) jwt.getPayload();

        List<?> groups = claims.get("cognito:groups", List.class);
        GrupoUsuario group = null;
        if (groups != null && !groups.isEmpty() && groups.getFirst() instanceof String groupStr) {
            try {
                group = GrupoUsuario.valueOf(groupStr);
            } catch (IllegalArgumentException ignored) {
                // Nenhum grupo conhecido para a aplicação
            }
        }

        return new TokenBasedUsuarioLogado(true,
                claims.get("name", String.class),
                claims.get("custom:cpf", String.class),
                claims.get("email", String.class),
                group,
                null,
                identityToken);
    }

    private class TokenBasedUsuarioLogado implements UsuarioLogado {
        private final boolean autenticado;
        private final String nome;
        private final String cpf;
        private final String email;
        private final GrupoUsuario grupo;
        private final String authError;
        private final String token;

        private TokenBasedUsuarioLogado(boolean autenticado,
                                        String nome, String cpf, String email, GrupoUsuario grupo,
                                        String authError, String token) {
            this.autenticado = autenticado;
            this.nome = nome;
            this.cpf = cpf;
            this.email = email;
            this.grupo = grupo;
            this.authError = authError;
            this.token = token;
        }

        @Override
        public boolean autenticado() {
            return autenticado;
        }

        @Override
        public String getNome() {
            if (!autenticado)
                throw new IllegalStateException("Usuario não autenticado");
            return nome;
        }

        @Override
        public String getCpf() {
            if (!autenticado)
                throw new IllegalStateException("Usuario não autenticado");
            return cpf;
        }

        @Override
        public String getEmail() {
            if (!autenticado)
                throw new IllegalStateException("Usuario não autenticado");
            return email;
        }

        @Override
        public GrupoUsuario getGrupo() {
            if (!autenticado)
                throw new IllegalStateException("Usuario não autenticado");
            return grupo;
        }

        @Override
        public String identityToken() {
            if (!autenticado)
                throw new IllegalStateException("Usuario não autenticado");
            return token;
        }

        @Override
        public String authError() {
            return authError;
        }
    }
}
