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
import java.time.temporal.TemporalAmount;

@Log4j2
public class TemporalAmountConverter implements ValueConverter<TemporalAmount> {
    @Override
    public TemporalAmount convert(String value) {
        TemporalAmount amount = null;
        try {
            if (value.length() == 1) {
                value = "1" + value;
            }

            String unit = value.substring(value.length() - 1);
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
                case "d":
                    amount = Duration.ofDays(l);
                    break;
                case "w":
                    amount = Duration.ofDays(l * 7);
                    break;
                case "M":
                    amount = Duration.ofDays(l * 30);
                    break;
                case "y":
                    amount = Duration.ofDays(l * 365);
                    break;
                default:
                    throw new IllegalArgumentException(
                            value + " is not a supported temporal " + "amount!");
            }
        } catch (Exception e) {
            String msg = "Cannot convert history \"" + value + "\" to temporal amount!";
            log.error(msg, e);
            throw new ValueConversionException(msg, e);
        }

        return amount;
    }

    @Override
    public Class<? extends TemporalAmount> valueType() {
        return TemporalAmount.class;
    }

    @Override
    public String valuePattern() {
        return "A number plus time unit (one of 's', 'm', 'h', 'd', 'w', 'M', or 'y')";
    }
}
