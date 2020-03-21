package org.grajagan.emporia;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JacksonObjectMapper extends ObjectMapper {

    public JacksonObjectMapper() {
        super();
        registerModule(new JavaTimeModule());
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
