package org.grajagan.aws;

/*-
 * #%L
 * Emporia Energy API Client
 * %%
 * Copyright (C) 2002 - 2020 Helge Weissig
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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

    private static final String REGION = "us-east-2";
    private static final String POOL_ID = "us-east-2_ghlOXVLi1";
    private static final String CLIENT_ID = "4qte47jbstod8apnfic0bunmrq";

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
            provider = new AwsCognitoRSAKeyProvider(REGION, POOL_ID);
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
        AuthenticationHelper authenticationHelper = new AuthenticationHelper(POOL_ID, CLIENT_ID);
        RespondToAuthChallengeResult result =
                authenticationHelper.performSRPAuthentication(username, password);
        authenticationResult = new AuthenticationResult(result.getAuthenticationResult());
        isValidated = false;
    }

    protected void refreshToken() throws Exception {
        log.info("refreshing " + authenticationResult.toString());
        InitiateAuthRequest initiateAuthRequest = new InitiateAuthRequest();
        initiateAuthRequest.setAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH);
        initiateAuthRequest.setClientId(CLIENT_ID);
        initiateAuthRequest
                .addAuthParametersEntry("REFRESH_TOKEN", authenticationResult.getRefreshToken());

        AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();
        AWSCognitoIdentityProvider cognitoIdentityProvider =
                AWSCognitoIdentityProviderClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                        .withRegion(Regions.fromName(REGION)).build();
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
