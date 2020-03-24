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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmporiaDownloaderTest {

    @Test
    void getConfiguration() throws Exception {
        String key = LoggingConfigurator.RAW;
        String optArg = "foobar";

        OptionParser parser = EmporiaDownloader.getOptionParser();
        List<String> optionList = new ArrayList<>();
        for (String option : EmporiaDownloader.REQUIRED_PARAMETERS) {
            optionList.add("--" + option);
            optionList.add(optArg);
        }

        Configuration configuration = getConfiguration(parser, optionList);

        assertFalse(configuration.containsKey(key),"We should not output raw data");

        optionList.add("--" + key);
        configuration = getConfiguration(parser, optionList);

        assertTrue(configuration.containsKey(key), "Configuration should contain the key" + key);
        assertEquals(EmporiaDownloader.DEFAULT_RAW_LOG_FILE, configuration.getString(key));

        optionList.add("true");
        assertEquals(EmporiaDownloader.DEFAULT_RAW_LOG_FILE, configuration.getString(key));

        optionList.remove(optionList.size() - 1);
        optionList.add(optArg);
        configuration = getConfiguration(parser, optionList);
        assertEquals(optArg, configuration.getString(key));

    }

    private Configuration getConfiguration(OptionParser parser, List<String> optionList)
            throws ConfigurationException {
        OptionSet optionSet = parser.parse(optionList.toArray(new String[0]));
        return EmporiaDownloader.getConfiguration(optionSet);
    }
}
