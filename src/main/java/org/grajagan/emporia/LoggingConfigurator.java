package org.grajagan.emporia;

import joptsimple.OptionSet;
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

    private LoggingConfigurator() {}

    public static void configure(org.apache.commons.configuration.Configuration configuration) {
        MainMapLookup.setMainArguments(configuration.getString(LOGFILE));
        if (configuration.containsKey(DEBUG)) {
            Configurator.setLevel("org.grajagan", Level.DEBUG);
        }

        if (configuration.containsKey(QUIET)) {
            Configurator.setRootLevel(Level.OFF);
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            final Configuration config = ctx.getConfiguration();
            config.getLoggers().get("org.grajagan").removeAppender("CONSOLE");
            ctx.updateLoggers();
        }

        log.debug("configured logging!");
    }
}
