package org.grajagan.emporia.model;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@ToString
@JsonPOJOBuilder(withPrefix = "")
public class Device {
    private Integer deviceGid;
    private String manufacturerDeviceId;
    private String manufacturer;
    private String model;
    private String firmware;
    private List<Device> devices = new ArrayList<>();
    private List<Channel> channels = new ArrayList<>();
}
