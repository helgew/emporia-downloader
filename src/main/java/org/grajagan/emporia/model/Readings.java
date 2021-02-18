package org.grajagan.emporia.model;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@ToString
@JsonPOJOBuilder(withPrefix = "")
@Log4j2
public class Readings {
    public static final String DEFAULT_TYPE = "INSTANT";
    public static final String DEFAULT_SCALE = "1S";
    public static final String DEFAULT_UNIT = "KilowattHours";

    private Instant start;
    private Instant end;
    private Instant firstUsageInstant;
    private String type = DEFAULT_TYPE;
    private String scale = DEFAULT_SCALE;
    private String unit = DEFAULT_UNIT;
    private Channel channel;
    private List<Float> usageList = new ArrayList<>();

    @JsonIgnore
    static Pattern pattern = Pattern.compile("(\\d+)(\\w+)");

    @JsonIgnore
    public Duration getInterval() {
        Matcher matcher = pattern.matcher(scale);
        if (matcher.find()) {
            Integer amount = new Integer(matcher.group(1));
            ChronoUnit chronoUnit = ChronoUnit.SECONDS;
            switch (matcher.group(2)) {
                case "S":
                    chronoUnit = ChronoUnit.SECONDS;
                    break;
                case "MIN":
                    chronoUnit = ChronoUnit.MINUTES;
                    break;
                case "H":
                    chronoUnit = ChronoUnit.HOURS;
                    break;
            }

            return Duration.of(amount, chronoUnit);
        }

        return null;
    }

    @JsonIgnore
    public SortedMap<Instant, Float> getDataPoints() {
        SortedMap<Instant, Float> dataPoints = new TreeMap<>();
        if (start == null || end == null || scale == null || usageList == null) {
            log.warn("Missing values: " + toString());
            return dataPoints;
        }

        Duration interval = getInterval();
        Float kwhToWattFactor = 3.6f / interval.getSeconds();

        Instant instant = start;
        for (Float data : usageList) {
            dataPoints.put(instant, data * kwhToWattFactor);
            instant = instant.plus(interval);
        }

        return dataPoints;
    }

    public void setFirstUsageInstant(Instant firstUsageInstant) {
        this.firstUsageInstant = firstUsageInstant;
        setStart(firstUsageInstant);
        calculateEnd();
    }

    public void setUsageList(List<Float> usageList) {
        this.usageList = usageList;
        calculateEnd();
    }

    private void calculateEnd() {
        if (firstUsageInstant != null) {
            Duration interval = getInterval();
            setEnd(start.plus(interval.getSeconds() * usageList.size(), ChronoUnit.SECONDS));
        }
    }
}
