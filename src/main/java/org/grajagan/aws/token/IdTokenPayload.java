package org.grajagan.aws.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Data
@JsonPOJOBuilder(withPrefix = "")
@ToString
@Log4j2
public class IdTokenPayload extends TokenPayload {
    private String name;
    private String email;
    private boolean email_verified;
    @JsonProperty("cognito:username")
    private String cognitoUsername;
    private String aud;
}
