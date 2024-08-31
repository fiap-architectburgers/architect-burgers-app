package com.example.gomesrodris.archburgers.adapters.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.io.Parser;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Locates signing key from Cognito
 * <p>
 * <a href="https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html">Reference</a>
 * <p>
 * Implemented per the doc recommendation:
 * <pre>
 * Amazon Cognito might rotate signing keys in your user pool. As a best practice, cache public keys in your app, using the kid as a cache key,
 * and refresh the cache periodically. Compare the kid in the tokens that your app receives to your cache.
 * If you receive a token with the correct issuer but a different kid, Amazon Cognito might have rotated the signing key. Refresh the cache
 * from your user pool jwks_uri endpoint.
 * </pre>
 */
class JwksKeyLocator implements Locator<Key> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwksKeyLocator.class);

    private final CognitoJwksApi cognitoJwksApi;

    private final Map<String, Key> keyCache = new HashMap<>();

    private final ReadWriteLock cacheControlLock = new ReentrantReadWriteLock();

    public JwksKeyLocator(CognitoJwksApi cognitoJwksApi) {
        this.cognitoJwksApi = cognitoJwksApi;
    }

    @Override
    public Key locate(Header header) {
        Object kidObj = header.get("kid");
        if (!(kidObj instanceof String kid))
            throw new RuntimeException("Unexpected JWT header format. Missing kid");

        Lock readLock = cacheControlLock.readLock();
        readLock.lock();
        try {
            if (keyCache.containsKey(kid))
                return keyCache.get(kid);
        } finally {
            readLock.unlock();
        }

        // Cache may be still uninitialized, or keys may have rotated. Reload cache

        Lock writeLock = cacheControlLock.writeLock();
        writeLock.lock();
        try {
            reloadKeyCache();

            return keyCache.get(kid);  // If not found now, that will be the final answer...
        } finally {
            writeLock.unlock();
        }
    }

    private void reloadKeyCache() {
        String json = cognitoJwksApi.getKnownSignaturesJson();

        Map<String, Key> updatedKeys = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Parser<Jwk<?>> jwkParser = Jwks.parser().build();

            Map<?, ?> signaturesResponse = mapper.readValue(json, Map.class);
            List<?> keysRaw = Objects.requireNonNull((List<?>) signaturesResponse.get("keys"));

            for (Object keySpec : keysRaw) {
                if (!(keySpec instanceof Map<?, ?> keySpacMap))
                    throw new RuntimeException("Unexpected JWT key format. Not an object");

                if (!(keySpacMap.get("kid") instanceof String kid))
                    throw new RuntimeException("Unexpected JWT key format. Missing kid");

                String keySpecJson = mapper.writeValueAsString(keySpacMap);
                Key key = jwkParser.parse(keySpecJson).toKey();

                updatedKeys.put(kid, key);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing response from Jwks endpoint. {} -- {}", e, json, e);
            throw new RuntimeException("Error parsing response from Jwks endpoint", e);
        }

        keyCache.clear();
        keyCache.putAll(updatedKeys);
    }
}
