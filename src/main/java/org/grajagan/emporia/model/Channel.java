package org.grajagan.emporia.model;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@JsonPOJOBuilder(withPrefix = "")
public class Channel {
    private Integer deviceGid;
    private String name;
    private String channelNum;
    private Float channelMultiplier;
}
