package com.example.gomesrodris.archburgers.adapters.auth;

import com.example.gomesrodris.archburgers.adapters.externalsystem.AwsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class CognitoJwksApi {
    private final URI jwksEndpointURI;

    private final HttpClient httpClient;

    @Autowired
    public CognitoJwksApi(Environment environment) {
        AwsConfig awsConfig = AwsConfig.loadFromEnv(environment);

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        /*
         * https://cognito-idp.<Region>.amazonaws.com/<userPoolId>/.well-known/jwks.json
         *
         * As specified in https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html
         */
        String uriStr = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                awsConfig.getAwsRegion(), awsConfig.getCognitoUserPoolId());

        try {
            jwksEndpointURI = new URI(uriStr);
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI error: " + uriStr, e);
        }
    }

    public String getKnownSignaturesJson() {
        var webRequest = HttpRequest.newBuilder()
                .uri(jwksEndpointURI)
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(webRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Error requesting identity signature info: " + e, e);
        }

        String body = response.body();

        if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 204) {
            throw new RuntimeException("Error in identity signature info request: " + response + " -- " + body);
        }

        return body;
    }
}
