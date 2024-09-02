package com.example.gomesrodris.archburgers.adapters.externalsystem;

import com.example.gomesrodris.archburgers.domain.entities.Cliente;
import com.example.gomesrodris.archburgers.domain.external.ProvedorAutenticacaoExterno;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ProvedorAutenticacaoCognito implements ProvedorAutenticacaoExterno, AutoCloseable {
    private final AwsConfig awsConfig;
    private final CognitoIdentityProviderClient cognitoClient;

    public ProvedorAutenticacaoCognito(Environment environment) {
        this.awsConfig = AwsConfig.loadFromEnv(environment);

        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Override
    public void registrarCliente(Cliente cliente, String senha) {
        List<AttributeType> userAttrsList = new ArrayList<>();

        userAttrsList.add(AttributeType.builder()
                .name("email")
                .value(cliente.email())
                .build());
        userAttrsList.add(AttributeType.builder()
                .name("custom:cpf")
                .value(cliente.cpf().cpfNum())
                .build());

        SignUpRequest signUpRequest = SignUpRequest.builder()
                .userAttributes(userAttrsList)
                .username(cliente.email())
                .clientId(awsConfig.getCognitoClientId())
                .password(senha)
                .secretHash(secretHash(cliente.email(), awsConfig.getCognitoClientId(), awsConfig.getCognitoClientSecret()))
                .build();

        SignUpResponse signUpResponse = cognitoClient.signUp(signUpRequest);

        if (!Boolean.TRUE.equals(signUpResponse.userConfirmed())) {
            AdminConfirmSignUpRequest confirmSignUpRequest = AdminConfirmSignUpRequest.builder()
                    .username(cliente.email())
                    .userPoolId(awsConfig.getCognitoUserPoolId())
                    .build();

            cognitoClient.adminConfirmSignUp(confirmSignUpRequest);
        }
    }

    private static String secretHash(String userName, String clientId, String clientSecret) {
        Mac hmac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256, clientSecret.getBytes(StandardCharsets.UTF_8));
        hmac.update((userName + clientId).getBytes(StandardCharsets.UTF_8));
        byte[] finalMac = hmac.doFinal();

        return Base64.getEncoder().encodeToString(finalMac);
    }

    @Override
    public void close() throws Exception {
        cognitoClient.close();
    }
}
