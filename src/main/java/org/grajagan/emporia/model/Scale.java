package org.grajagan.emporia.model;

/*-
 * #%L
 * Emporia Energy API Client
 * %%
 * Copyright (C) 2002 - 2021 Helge Weissig
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

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.grajagan.emporia.TemporalAmountConverter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

@Log4j2
@Getter
public class Scale {
    private final String unit;
    private final TemporalAmount amount;

    public Scale(String unit) {
        this.unit = unit;

        try {
            amount = new TemporalAmountConverter().convert(unit);
        } catch (Exception e) {
            throw new IllegalArgumentException(unit + " is not a supported unit!");
        }
    }

    public ChronoUnit getChronoUnit() {
        ChronoUnit chronoUnit = ChronoUnit.SECONDS;
        switch (unit) {
            case "s":
                chronoUnit = ChronoUnit.SECONDS;
                break;
            case "m":
                chronoUnit = ChronoUnit.MINUTES;
                break;
            case "h":
                chronoUnit = ChronoUnit.HOURS;
                break;
            case "d":
                chronoUnit = ChronoUnit.DAYS;
                break;
            case "w":
                chronoUnit = ChronoUnit.WEEKS;
                break;
            case "M":
                chronoUnit = ChronoUnit.MONTHS;
                break;
            case "y":
                chronoUnit = ChronoUnit.YEARS;
                break;
        }
        return chronoUnit;
    }

    public Duration toInterval() {
        return Duration.of(1, getChronoUnit());
    }

    @Override
    public String toString() {
        switch (unit) {
            case "s":
                return "1SEC";
            case "m":
                return "1MIN";
            case "M":
                return "1MON";
            default:
                return "1" + unit.toUpperCase();
        }
    }
}
