package org.grajagan.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.RespondToAuthChallengeResult;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Data
@Log4j2
@Builder
@ToString
public class CognitoAuthenticationManager {
    private final String username;
    @ToString.Exclude
    private final String password;
    private final String region;
    private final String poolId;
    private final String clientId;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private AuthenticationResult authenticationResult;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private boolean isValidated;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private AwsCognitoRSAKeyProvider provider;

    public String getIdentityToken() {
        log.debug("retrieving ID token for " + toString());
        if (authenticationResult == null || isExpired(authenticationResult) || isInvalid(
                authenticationResult)) {
            try {
                authenticate();
            } catch (Exception e) {
                log.error("Cannot authenticate with the given credentials " + toString(), e);
            }
        }

        return authenticationResult.getIdToken();
    }

    protected boolean isExpired(AuthenticationResult authenticationResult) {
        if (authenticationResult == null) {
            log.warn("authentication result is null!");
            isValidated = false;
            return true;
        }

        Instant expirationTime = authenticationResult.getExpirationTime();
        log.debug("expiration time: " + expirationTime);
        return expirationTime.isBefore(Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    protected boolean isInvalid(AuthenticationResult authenticationResult) {
        if (authenticationResult == null) {
            log.warn("authentication result is null!");
            isValidated = false;
            return false;
        }

        log.debug("verifying " + authenticationResult.toString());
        if (isValidated) {
            log.debug("using cached validation");
            return false;
        }

        if (provider == null) {
            provider = new AwsCognitoRSAKeyProvider(region, poolId);
        }

        Algorithm algorithm = Algorithm.RSA256(provider);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            jwtVerifier.verify(authenticationResult.getIdToken());
        } catch (JWTVerificationException e) {
            log.error("Invalid ID token!", e);
            isValidated = false;
            return true;
        }

        log.info("authtoken: " + authenticationResult.getIdToken());
        log.info("refresh token: " + authenticationResult.getRefreshToken());
        isValidated = true;
        return false;
    }

    protected void authenticate() throws Exception {
        log.debug("authenticating");
        if (authenticationResult == null || isInvalid(authenticationResult)) {
            login();
        } else if (isExpired(authenticationResult)) {
            refreshToken();
        }

        if (isInvalid(authenticationResult)) {
            throw new InvalidParameterException(
                    "Unable to authenticate with the given credentials " + toString());
        }
    }

    protected void login() throws Exception {
        log.info("logging in");
        AuthenticationHelper authenticationHelper = new AuthenticationHelper(poolId, clientId);
        RespondToAuthChallengeResult result =
                authenticationHelper.performSRPAuthentication(username, password);
        authenticationResult = new AuthenticationResult(result.getAuthenticationResult());
        isValidated = false;
    }

    protected void refreshToken() throws Exception {
        log.info("refreshing " + authenticationResult.toString());
        InitiateAuthRequest initiateAuthRequest = new InitiateAuthRequest();
        initiateAuthRequest.setAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH);
        initiateAuthRequest.setClientId(clientId);
        initiateAuthRequest
                .addAuthParametersEntry("REFRESH_TOKEN", authenticationResult.getRefreshToken());

        AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();
        AWSCognitoIdentityProvider cognitoIdentityProvider =
                AWSCognitoIdentityProviderClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                        .withRegion(Regions.fromName(region)).build();
        InitiateAuthResult initiateAuthResult =
                cognitoIdentityProvider.initiateAuth(initiateAuthRequest);

        authenticationResult
                .updateAccessToken(initiateAuthResult.getAuthenticationResult().getAccessToken());

        String newIdToken = initiateAuthResult.getAuthenticationResult().getIdToken();
        if (newIdToken.equals(authenticationResult.getIdToken())) {
            log.warn("ID token identical after refresh");
        }

        authenticationResult.updateIdToken(newIdToken);
        isValidated = false;

        log.debug("new expiration time: " + authenticationResult.getExpirationTime());
    }
}
