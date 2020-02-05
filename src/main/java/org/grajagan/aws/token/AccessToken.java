package org.grajagan.aws.token;

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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.InvalidParameterException;

@Log4j2
public class AccessToken extends Token {
    @Getter
    private final AccessTokenPayload payload;

    public AccessToken(String signature, TokenHeader header, AccessTokenPayload payload) {
        super(signature, header);
        this.payload = payload;
    }

    public static AccessToken fromJWT(String jwt) {
        DecodedJWT decodedJWT = JWT.decode(jwt);
        AccessTokenPayload payload;
        try {
            payload = objectMapper
                    .readValue(toJSON(decodedJWT.getPayload()), AccessTokenPayload.class);
        } catch (IOException e) {
            log.error("Cannot extract AccessTokenPayload from " + jwt, e);
            throw new InvalidParameterException("Cannot extract AccessTokenPayload from " + jwt);
        }
        TokenHeader header = fromJWT(decodedJWT);
        return new AccessToken(decodedJWT.getSignature(), header, payload);
    }
}
