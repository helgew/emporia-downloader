package org.grajagan.aws.token;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Data
@JsonPOJOBuilder(withPrefix = "")
@ToString
@Log4j2
public class TokenHeader {
    private String kid;
    private String alg;
}
