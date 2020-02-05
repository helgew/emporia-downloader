package org.grajagan.aws;

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
