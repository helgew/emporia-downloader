package org.grajagan.aws.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.InvalidParameterException;

@Log4j2
public class IdToken extends Token {
    @Getter
    private final IdTokenPayload payload;

    public IdToken(String signature, TokenHeader header, IdTokenPayload payload) {
        super(signature, header);
        this.payload = payload;
    }

    public static IdToken fromJWT(String jwt) {
        DecodedJWT decodedJWT = JWT.decode(jwt);
        IdTokenPayload payload;
        try {
            payload = objectMapper
                    .readValue(toJSON(decodedJWT.getPayload()), IdTokenPayload.class);
        } catch (IOException e) {
            log.error("Cannot extract IdTokenPayload from " + jwt, e);
            throw new InvalidParameterException("Cannot extract IdTokenPayload from " + jwt);
        }
        TokenHeader header = fromJWT(decodedJWT);
        return new IdToken(decodedJWT.getSignature(), header, payload);
    }
}
