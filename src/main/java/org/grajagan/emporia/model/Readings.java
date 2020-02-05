package org.grajagan.emporia.model;

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
    public static final String DEFAULT_UNIT = "WATTS";

    private Instant start;
    private Instant end;
    private String type = DEFAULT_TYPE;
    private String scale = DEFAULT_SCALE;
    private String unit = DEFAULT_UNIT;
    private Channel channel;
    private List<Float> usage = new ArrayList<>();

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
        if (start == null || end == null || scale == null || usage == null) {
            log.warn("Missing values: " + toString());
            return dataPoints;
        }

        Duration interval = getInterval();

        Instant instant = start;
        for (Float data : usage) {
            dataPoints.put(instant, data);
            instant = instant.plus(interval);
        }

        return dataPoints;
    }
}
