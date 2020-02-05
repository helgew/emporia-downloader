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
