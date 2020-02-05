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

import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.grajagan.aws.token.AccessToken;
import org.grajagan.aws.token.IdToken;

import java.time.Instant;

@Data
public class AuthenticationResult {
    @Delegate
    private AuthenticationResultType authenticationResultType;

    @Setter(AccessLevel.PRIVATE)
    private IdToken idTokenContent;

    @Setter(AccessLevel.PRIVATE)
    private AccessToken accessTokenContent;

    public AuthenticationResult(AuthenticationResultType authenticationResultType) {
        this.authenticationResultType = authenticationResultType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getIdToken() != null) {
            IdToken content = getIdTokenContent();
            sb.append("IdToken: ").append(content).append(",");
        }

        if (getExpiresIn() != null) {
            sb.append("ExpiresIn: ").append(getExpiresIn());
            if (getIdToken() != null) {
                sb.append(" (").append(getExpirationTime()).append(")");
            }
            sb.append(",");
        }

        if (getAccessToken() != null) {
            AccessToken content = getAccessTokenContent();
            sb.append("AccessToken: ").append(content).append(",");
        }

        if (getTokenType() != null) {
            sb.append("TokenType: ").append(getTokenType()).append(",");
        }

        if (getRefreshToken() != null) {
            sb.append("RefreshToken: ").append(getRefreshToken(), 0, 20).append("[...],");
        }
        if (getNewDeviceMetadata() != null) {
            sb.append("NewDeviceMetadata: ").append(getNewDeviceMetadata());
        }

        sb.append("}");

        return sb.toString();
    }

    private IdToken getIdTokenContent() {
        if (idTokenContent == null && getIdToken() != null) {
            idTokenContent = IdToken.fromJWT(getIdToken());
        }
        return idTokenContent;
    }

    private AccessToken getAccessTokenContent() {
        if (accessTokenContent == null && getAccessToken() != null) {
            accessTokenContent = AccessToken.fromJWT(getAccessToken());
        }
        return accessTokenContent;
    }

    public Instant getExpirationTime() {
        return getIdTokenContent().getPayload().getExpirationTime();
    }

    public void updateIdToken(String idToken) {
        authenticationResultType.setIdToken(idToken);
        idTokenContent = IdToken.fromJWT(idToken);
    }

    public void updateAccessToken(String accessToken) {
        authenticationResultType.setAccessToken(accessToken);
        accessTokenContent = AccessToken.fromJWT(accessToken);
    }
}
