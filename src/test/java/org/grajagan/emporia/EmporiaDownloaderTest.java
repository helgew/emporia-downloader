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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

        optionList.add("--config");
        optionList.add(FOOBAR);
    }

    @Test
    public void testRawOutputConfiguration() throws ConfigurationException {
        String key = LoggingConfigurator.RAW;

        optionList.add("--" + key);
        Configuration configuration = getConfiguration(parser, optionList);

        assertTrue(configuration.containsKey(key), "Configuration should contain the key " + key);
        assertTrue(configuration.getProperty(key) instanceof Boolean,
                "Property should be a boolean");

        optionList.add("true");
        configuration = getConfiguration(parser, optionList);

        assertTrue(configuration.getProperty(key) instanceof Boolean,
                "Property should be a boolean");
    }

    private Configuration getConfiguration(OptionParser parser, List<String> optionList)
            throws ConfigurationException {
        OptionSet optionSet = parser.parse(optionList.toArray(new String[0]));
        return EmporiaDownloader.getConfiguration(optionSet);
    }
}
