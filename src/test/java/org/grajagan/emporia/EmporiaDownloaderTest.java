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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmporiaDownloaderTest {

    OptionParser parser;
    List<String> optionList;

    static final String FOOBAR = "foobar";

    @BeforeEach
    public void setUp() {

        parser = EmporiaDownloader.getOptionParser();
        optionList = new ArrayList<>();
        for (String option : EmporiaDownloader.REQUIRED_PARAMETERS) {
            optionList.add("--" + option);
            optionList.add(FOOBAR);
        }
    }

    @Test
    public void testRawOutputConfiguration() throws ConfigurationException {
        String key = LoggingConfigurator.RAW;

        Configuration configuration = getConfiguration(parser, optionList);

        assertFalse(configuration.containsKey(key), "We should not output raw data");

        optionList.add("--" + key);
        configuration = getConfiguration(parser, optionList);

        assertTrue(configuration.containsKey(key), "Configuration should contain the key " + key);
        assertTrue(configuration.getProperty(key) instanceof String,
                "Property should be a string");
        assertTrue(configuration.getString(key).isEmpty(), "Property should be empty");

        optionList.add("true");
        assertTrue(configuration.getString(key).isEmpty(), "Property should be empty");

        popNshift(FOOBAR);

        configuration = getConfiguration(parser, optionList);
        assertEquals(FOOBAR, configuration.getString(key));
    }

    private void popNshift(String argument) {
        optionList.remove(optionList.size() - 1);
        optionList.add(argument);
    }

    @Test
    public void testOffsetConfiguration() throws ConfigurationException {
        String key = EmporiaDownloader.OFFSET;
        Configuration configuration = getConfiguration(parser, optionList);
        assertTrue(configuration.containsKey(key), "The key " + key + " is missing!");
        assertTrue(configuration.getProperty(key) instanceof Instant,
                "The property should be an Instant");

        optionList.add("--" + key);

        assertThrows(OptionException.class, () -> getConfiguration(parser, optionList),
                "The property " + key + " requires an argument");

        optionList.add("1h");
        configuration = getConfiguration(parser, optionList);
        Instant offset = (Instant) configuration.getProperty(key);
        Instant now = Instant.now();
        assertTrue(
                now.minus(offset.toEpochMilli(), ChronoUnit.MILLIS).truncatedTo(ChronoUnit.MINUTES)
                        .equals(Instant.EPOCH.plus(Duration.ofHours(1))));
    }

    private Configuration getConfiguration(OptionParser parser, List<String> optionList)
            throws ConfigurationException {
        OptionSet optionSet = parser.parse(optionList.toArray(new String[0]));
        return EmporiaDownloader.getConfiguration(optionSet);
    }
}
