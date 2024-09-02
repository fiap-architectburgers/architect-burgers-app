package com.example.gomesrodris.archburgers.adapters.auth;

import com.example.gomesrodris.archburgers.domain.auth.GrupoUsuario;
import io.jsonwebtoken.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioLogadoTokenParserTest {
    private CognitoJwksApi cognitoJwksApi;
    private Clock expirationCheckClock;

    private UsuarioLogadoTokenParser parser;

    @BeforeEach
    void setUp() {
        cognitoJwksApi = mock(CognitoJwksApi.class);

        expirationCheckClock = mock(Clock.class);

        parser = new UsuarioLogadoTokenParser(cognitoJwksApi, expirationCheckClock);
    }

    @Test
    void verificarUsuarioLogado_headerNotPresent() {
        var usuario = parser.verificarUsuarioLogado(new HttpHeaders());
        assertThat(usuario.autenticado()).isFalse();

        assertThrows(IllegalStateException.class, usuario::getNome);
        assertThrows(IllegalStateException.class, usuario::getCpf);
        assertThrows(IllegalStateException.class, usuario::getEmail);
        assertThrows(IllegalStateException.class, usuario::getGrupo);
        assertThrows(IllegalStateException.class, usuario::identityToken);

        assertThat(usuario.authError()).isEqualTo("IdentityToken is missing");
    }

    @Test
    void verificarUsuarioLogado_ok() {
        when(cognitoJwksApi.getKnownSignaturesJson()).thenReturn(EXAMPLE_SIGNATURES_JSON);

        when(expirationCheckClock.now()).thenReturn(new Date(1725131899000L)); // Fixed time before the expiration of the example token

        String token = "eyJraWQiOiIzXC9EQk5VQThYZWgzdTRFRnN6XC9JNlJOdTlZNkkyUGZqT2FFUTBkMlRXRWc9IiwiYWxnIjoiUlMyNTYifQ" +
                ".eyJzdWIiOiI5NDU4YzQ2OC1iMDIxLTcwYzgtOWVhYy0xMzRhZDg5YmJmYTIiLCJjb2duaXRvOmdyb3VwcyI6WyJDbGllbnRlQ2FkYXN0cmFkbyJdLCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0xLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMV85NldGY3dCOEUiLCJjb2duaXRvOnVzZXJuYW1lIjoiOTQ1OGM0NjgtYjAyMS03MGM4LTllYWMtMTM0YWQ4OWJiZmEyIiwib3JpZ2luX2p0aSI6ImRjODI1NDI4LTI1NDktNGNjOC05MDg0LWFhM2I4NTNmNmFmNiIsImF1ZCI6IjZ2Y2" +
                "tqb3ZybjF1dGFsaDJpZXJzOXExNGNhIiwiZXZlbnRfaWQiOiIxOWJmY2I2NC0xOGUyLTQ4NjEtYmI4ZS1iYTlmYWQ4YzIzZDkiLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTcyNTEzMTY2OCwibmFtZSI6IkR1ZHVIYW1idXJnZXIiLCJleHAiOjE3MjUxMzUyNjgsImN1c3RvbTpjcGYiOiIxMjMzMjExMjM0MCIsImlhdCI6MTcyNTEzMTY2OCwianRpIjoiZjc4MjhmNmMtY2U2NS00MWQyLTg5NWYtZTBhMzg0OWM5MTI2IiwiZW1haWwiOiJkdWR1QGV4YW1wbGUuY29tIn0" +
                ".sx3DrKFZkIl94mAGfmjKEUTtJYOrANK4U-qZPFvkshx8BmjmFupUitP8Is6ciI7R0fsZyFXt2qJQAyl5pNT_qu9vdUYcGoHxIUxlfCwAveD609SAbGFq7bcvOZ90ulsvypwLJPVJkBgsBDoT_vcisa7GpS19hh0xZWPIVEDzENLZCbaSQo0dcr3Vq03io4bNAOASUBzVoiWzz5BKIY50G0xw6WZIix0uwsI1GewJGU3eqchDWWAbDRD8ZfbHjy8HiD-haLTnj_Xq1ZIIUThx_95L_ltUXIJZC0rXqjZOdE-ero04obQF92sqM62P1xZKxxEBi7ETuQvC4vHmgnd_Fw";

        HttpHeaders headers = new HttpHeaders();
        headers.put(UsuarioLogadoTokenParser.HEADER_NAME, List.of(token));

        var usuario = parser.verificarUsuarioLogado(headers);

        assertThat(usuario.autenticado()).isTrue();
        assertThat(usuario.getNome()).isEqualTo("DuduHamburger");
        assertThat(usuario.getCpf()).isEqualTo("12332112340");
        assertThat(usuario.getEmail()).isEqualTo("dudu@example.com");
        assertThat(usuario.getGrupo()).isEqualTo(GrupoUsuario.ClienteCadastrado);
        assertThat(usuario.identityToken()).isEqualTo(token);
        assertThat(usuario.authError()).isNull();
    }

    @Test
    void verificarUsuarioLogado_assinaturaTokenInvalida() {
        when(cognitoJwksApi.getKnownSignaturesJson()).thenReturn(EXAMPLE_SIGNATURES_JSON);

        when(expirationCheckClock.now()).thenReturn(new Date(1725131899000L)); // Fixed time before the expiration of the example token

        HttpHeaders headers = new HttpHeaders();
        headers.put(UsuarioLogadoTokenParser.HEADER_NAME, List.of( // Token alterado com uma assinatura incorreta
                "eyJraWQiOiIzXC9EQk5VQThYZWgzdTRFRnN6XC9JNlJOdTlZNkkyUGZqT2FFUTBkMlRXRWc9IiwiYWxnIjoiUlMyNTYifQ" +
                        ".eyJzdWIiOiI5NDU4YzQ2OC1iMDIxLTcwYzgtOWVhYy0xMzRhZDg5YmJmYTIiLCJjb2duaXRvOmdyb3VwcyI6WyJDbGllbnRlQ2FkYXN0cmFkbyJdLCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0xLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMV85NldGY3dCOEUiLCJjb2duaXRvOnVzZXJuYW1lIjoiOTQ1OGM0NjgtYjAyMS03MGM4LTllYWMtMTM0YWQ4OWJiZmEyIiwib3JpZ2luX2p0aSI6ImRjODI1NDI4LTI1NDktNGNjOC05MDg0LWFhM2I4NTNmNmFmNiIsImF1ZCI6IjZ2Y2" +
                        "tqb3ZybjF1dGFsaDJpZXJzOXExNGNhIiwiZXZlbnRfaWQiOiIxOWJmY2I2NC0xOGUyLTQ4NjEtYmI4ZS1iYTlmYWQ4YzIzZDkiLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTcyNTEzMTY2OCwibmFtZSI6IkR1ZHVIYW1idXJnZXIiLCJleHAiOjE3MjUxMzUyNjgsImN1c3RvbTpjcGYiOiIxMjMzMjExMjM0MCIsImlhdCI6MTcyNTEzMTY2OCwianRpIjoiZjc4MjhmNmMtY2U2NS00MWQyLTg5NWYtZTBhMzg0OWM5MTI2IiwiZW1haWwiOiJkdWR1QGV4YW1wbGUuY29tIn0" +
                        ".ASSINATURAinvalidaASSINATURAinvalidaASSINATURAinvalidaASSINATURAinvalidaASSINATURAinvalidaYcGoHxIUxlfCwAveD609SAbGFq7bcvOZ90ulsvypwLJPVJkBgsBDoT_vcisa7GpS19hh0xZWPIVEDzENLZCbaSQo0dcr3Vq03io4bNAOASUBzVoiWzz5BKIY50G0xw6WZIix0uwsI1GewJGU3eqchDWWAbDRD8ZfbHjy8HiD-haLTnj_Xq1ZIIUThx_95L_ltUXIJZC0rXqjZOdE-ero04obQF92sqM62P1xZKxxEBi7ETuQvC4vHmgnd_Fw"
        ));

        var usuario = parser.verificarUsuarioLogado(headers);

        assertThat(usuario.autenticado()).isFalse();
        assertThat(usuario.authError()).startsWith("Erro ao validar IdentityToken: JWT signature does not match locally computed signature");
    }

    private static final String EXAMPLE_SIGNATURES_JSON = """
            {
              "keys": [
                {
                  "alg": "RS256",
                  "e": "AQAB",
                  "kid": "3/DBNUA8Xeh3u4EFsz/I6RNu9Y6I2PfjOaEQ0d2TWEg=",
                  "kty": "RSA",
                  "n": "tuz4IHRCUfCYkRC3h9fxGXkufLwKm14KAYtu-tvQ0B1ifsGxIpXWXEGhvXxN65gxc2KENgwqK8BZ9zKRKxRLUly9O-6-eX9mHTgtKiB94wMCkEc83fYSlZ6IEQ7fdjJZJP8lTHx3Q4dOkytrKZ804nvlT_69L46Y_387zAdKs8bOT_3LcK7gN3E5pTMlKNJAUaPyptu8hoGZfk1KmxigrLGNn5OZ3MwHncScFrhxzy4Nd-bpGsSSwZdL6MWxykdsLKqVRWqgpyQQoWEEIVRzdzGyli7951tBaptmu95jdXh-4LRH_z0_mdRe2allzfD-e5rR6FnQrHyA8LvOKkcYaQ",
                  "use": "sig"
                },
                {
                  "alg": "RS256",
                  "e": "AQAB",
                  "kid": "J6PLlzgv3tK7ePZrTiq7Flr32X8xxTLz/TKNO6wRBHA=",
                  "kty": "RSA",
                  "n": "o3s6gv5YkvaieTjbpd_5MPjSNH6ABCTKqKt6lz5lQl5k6ufpqGVsHC5_rgraKYymgQNFfvRJRSc2JBdmBOTpAilcXztr8T9ngjfrGD2GfHC8RfJPIgKVArRrmCgy9wVB9v2yA4a9lMAAJUNClEvEmTyyoi2ji8nmBx0nLdwrJH-mk1-UQnJG_eFfHYpeuf2uCmyWFCXQ1nt7JwJ-aB-YD0MHrwz5miY00Q6uw0bW7oI4jUSHppFQlRJm8ynqFBW_FJLc4ZU70NjDH_qnK1nAkQt7Y2oMp0KokOI68C7vljkXu2dhInXGldU4IM24c-9BCBB2PwzhCgTjzI7-OvyxjQ",
                  "use": "sig"
                }
              ]
            }""";
}