package org.grajagan.aws;

import lombok.extern.log4j.Log4j2;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Configuration;
import java.io.IOException;

@Log4j2
public class AuthTokenClientRequestFilter implements ClientRequestFilter {

    public static final String AUTHENTICATION_MANAGER = "authentication manager";

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        Configuration configuration = clientRequestContext.getConfiguration();
        try {
            CognitoAuthenticationManager authenticationManager =
                    (CognitoAuthenticationManager) configuration.getProperties().get(AUTHENTICATION_MANAGER);
            clientRequestContext.getHeaders().putSingle("authtoken", authenticationManager.getIdentityToken());
            clientRequestContext.getHeaders().putSingle("User-Agent", "Dart/2.5 (dart:io)");
        } catch (Exception e) {
            log.error("Cannot insert authtoken header", e);
        }
    }
}
