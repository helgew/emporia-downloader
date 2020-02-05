package org.grajagan.aws.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.time.Instant;

@Data
@JsonPOJOBuilder(withPrefix = "")
@ToString
@Log4j2
public class TokenPayload {
    private String event_id;
    private URL iss;
    private String sub;
    private String token_use;
    @JsonProperty("auth_time")
    private Instant authorizationTime;
    @JsonProperty("exp")
    private Instant expirationTime;
    private Instant iat;
}
