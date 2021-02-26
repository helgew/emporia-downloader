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

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

@Log4j2
public class LoggingConfigurator {
    public static final String LOGFILE = "logfile";
    public static final String DEBUG = "debug";
    public static final String QUIET = "quiet";
    public static final String RAW = "raw";

    private LoggingConfigurator() {}

    public static void configure(org.apache.commons.configuration.Configuration configuration) {
        MainMapLookup.setMainArguments(configuration.getString(LOGFILE));

        if (configuration.getBoolean(DEBUG, false)) {
            Configurator.setLevel("org.grajagan", Level.DEBUG);
        }

        if (configuration.getBoolean(QUIET, true)) {
            Configurator.setRootLevel(Level.OFF);
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            final Configuration config = ctx.getConfiguration();
            config.getLoggers().get("org.grajagan").removeAppender("CONSOLE");
            ctx.updateLoggers();
        }

        if (configuration.containsKey(RAW)) {
            Configurator.setLevel("org.grajagan.emporia.api.LogJsonInterceptor", Level.TRACE);
        }

        log.debug("configured logging!");
    }
}
