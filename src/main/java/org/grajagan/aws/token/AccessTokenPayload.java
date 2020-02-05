package org.grajagan.aws.token;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Data
@JsonPOJOBuilder(withPrefix = "")
@ToString
@Log4j2
public class AccessTokenPayload extends TokenPayload {
    private String scope;
    private String jti;
    private String client_id;
    private String username;
}
