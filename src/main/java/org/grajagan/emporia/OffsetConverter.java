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

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

@Log4j2
public class OffsetConverter implements ValueConverter<Temporal> {
    @Override
    public Instant convert(String value) {
        TemporalAmount amount = null;
        try {
            String unit = value.substring(value.length() - 1).toLowerCase();
            Long l = Long.parseLong(value.substring(0, value.length() - 1));
            switch (unit) {
                case "s":
                    amount = Duration.ofSeconds(l);
                    break;
                case "m":
                    amount = Duration.ofMinutes(l);
                    break;
                case "h":
                    amount = Duration.ofHours(l);
                    break;
            }
        } catch (Exception e) {
            String msg = "Cannot convert offset \"" + value + "\" to temporal amount!";
            log.error(msg, e);
            throw new ValueConversionException(msg, e);
        }

        return Instant.now().minus(amount);
    }

    @Override
    public Class<? extends Instant> valueType() {
        return Instant.class;
    }

    @Override
    public String valuePattern() {
        return null;
    }
}
