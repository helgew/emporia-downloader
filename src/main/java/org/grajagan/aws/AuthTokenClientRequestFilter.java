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
