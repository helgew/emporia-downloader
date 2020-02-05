package org.grajagan.aws.token;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.security.InvalidParameterException;

@Data
@Log4j2
public class Token {
    private final String signature;
    private final TokenHeader header;

    protected static ObjectMapper objectMapper = new ObjectMapper();
    static { objectMapper.registerModule(new JavaTimeModule()); }

    public static TokenHeader fromJWT(DecodedJWT decodedJWT) {
        TokenHeader header;
        try {
            header =
                    objectMapper.readValue(toJSON(decodedJWT.getHeader()), TokenHeader.class);
        } catch (IOException e) {
            log.error("Cannot extract TokenHeader from " + decodedJWT.getHeader(), e);
            throw new InvalidParameterException("Cannot extract TokenHeader from " + decodedJWT.getHeader());
        }

        return header;
    }

    protected static String toJSON(String base64String) {
        return new String(Base64.decodeBase64(base64String), Charsets.UTF_8);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Token(Signature=");
        sb.append(getSignature(), 0, 20).append("[...],");
        sb.append(header).append(")");
        return sb.toString();
    }
}
