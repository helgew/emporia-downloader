package org.grajagan.aws.token;

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
