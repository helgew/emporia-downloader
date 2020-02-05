package org.grajagan.emporia.model;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@ToString
@JsonPOJOBuilder(withPrefix = "")
public class Customer {
    private Integer customerGid;
    private String email;
    private String firstName;
    private String lastName;
    private Date createdAt;
    private List<Device> devices = new ArrayList<>();
}
