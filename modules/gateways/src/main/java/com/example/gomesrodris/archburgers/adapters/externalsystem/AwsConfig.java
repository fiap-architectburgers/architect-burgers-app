package com.example.gomesrodris.archburgers.adapters.externalsystem;

import com.example.gomesrodris.archburgers.domain.utils.StringUtils;
import org.springframework.core.env.Environment;

public class AwsConfig {
    private final String awsRegion;
    private final String cognitoUserPoolId;
    private final String cognitoClientId;
    private final String cognitoClientSecret;

    private AwsConfig(String awsRegion, String cognitoUserPoolId, String cognitoClientId, String cognitoClientSecret) {
        this.awsRegion = awsRegion;
        this.cognitoUserPoolId = cognitoUserPoolId;
        this.cognitoClientId = cognitoClientId;
        this.cognitoClientSecret = cognitoClientSecret;
    }

    public static AwsConfig loadFromEnv(Environment environment) {
        String awsRegion = environment.getProperty("archburgers.integration.aws.region");
        String userPoolId = environment.getProperty("archburgers.integration.cognito.userPoolId");
        String cognitoClientId = environment.getProperty("archburgers.integration.cognito.clientId");
        String cognitoClientSecret = environment.getProperty("archburgers.integration.cognito.clientSecret");

        if (StringUtils.isEmpty(awsRegion))
            throw new IllegalStateException("archburgers.integration.aws.region not set");
        if (StringUtils.isEmpty(userPoolId))
            throw new IllegalStateException("archburgers.integration.cognito.userPoolId not set");
        if (StringUtils.isEmpty(cognitoClientId))
            throw new IllegalStateException("archburgers.integration.cognito.clientId not set");
        if (StringUtils.isEmpty(cognitoClientSecret))
            throw new IllegalStateException("archburgers.integration.cognito.clientSecret not set");

        return new AwsConfig(awsRegion, userPoolId, cognitoClientId, cognitoClientSecret);
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getCognitoUserPoolId() {
        return cognitoUserPoolId;
    }

    public String getCognitoClientId() {
        return cognitoClientId;
    }

    public String getCognitoClientSecret() {
        return cognitoClientSecret;
    }
}
