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

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.grajagan.emporia.api.EmporiaAPIService;
import org.grajagan.emporia.influxdb.InfluxDBLoader;
import org.grajagan.emporia.model.Channel;
import org.grajagan.emporia.model.Customer;
import org.grajagan.emporia.model.Device;
import org.grajagan.emporia.model.Readings;
import org.grajagan.emporia.model.Scale;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

@Log4j2
public class EmporiaDownloader {
    private static final String HELP_ARG = "help";

    private static final String CONFIGURATION_FILE = "config";

    private static final String INFLUX_URL = "influx-url";
    private static final String INFLUX_PORT = "influx-port";
    private static final String INFLUX_USER = "influx-user";
    private static final String INFLUX_PASS = "influx-password";
    private static final String INFLUX_DB = "influx-db";
    private static final String DISABLE_INFLUX = "disable-influx";

    static final String HISTORY = "history";

    // keeping this for backwards compatibility
    static final String OFFSET = "offset";

    private static final String SLEEP = "sleep";

    static final String DEFAULT_CONFIGURATION_FILE =
            Paths.get("config.properties").toAbsolutePath().toString();

    static final String DEFAULT_LOCAL_HOST = "localhost";
    static final String DEFAULT_INFLUX_URL = "http://" + DEFAULT_LOCAL_HOST;
    static final int DEFAULT_INFLUX_PORT = 8086;
    static final String DEFAULT_INFLUX_DB = "electricity";

    static final String DEFAULT_LOG_FILE =
            Paths.get("application.log").toAbsolutePath().toString();

    static final Integer DEFAULT_SLEEP = 5;

    static final List<String> REQUIRED_PARAMETERS = new ArrayList<>();
    static final List<String> HAS_DEFAULT_VALUES = new ArrayList<>();

    static {
        REQUIRED_PARAMETERS
                .addAll(Arrays.asList(EmporiaAPIService.USERNAME, EmporiaAPIService.PASSWORD));

        HAS_DEFAULT_VALUES.addAll(Arrays.asList(SLEEP, HISTORY, EmporiaAPIService.SCALE));
    }

    private Configuration configuration;

    private Map<Channel, Instant> lastDataPoint = new HashMap<>();

    private EmporiaAPIService service;

    public static void main(String[] argv) throws Exception {

        OptionParser parser = getOptionParser();
        OptionSet options = parser.parse(argv);
        if (options.has(HELP_ARG)) {
            printHelp(parser);
            System.exit(0);
        }

        if (!options.has(DISABLE_INFLUX)) {
            REQUIRED_PARAMETERS.add(INFLUX_URL);
            REQUIRED_PARAMETERS.add(INFLUX_DB);
            REQUIRED_PARAMETERS.add(INFLUX_PORT);
        }

        Configuration configuration = null;
        try {
            configuration = getConfiguration(options);
        } catch (ConfigurationException e) {
            printHelp(parser);
            System.exit(1);
        }

        LoggingConfigurator.configure(configuration);

        if (log.isDebugEnabled()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            ConfigurationUtils.dump(configuration, printWriter);
            printWriter.flush();
            String conf = stringWriter.toString();
            conf = conf.replaceAll("password=[^\\n]+", "password=*****");
            conf = conf.replaceAll("\\n", ", ");
            log.debug("configuration: " + conf);
        }

        Security.setProperty("networkaddress.cache.ttl", "60");

        EmporiaDownloader downloader = new EmporiaDownloader(configuration);
        downloader.run();
    }

    static OptionParser getOptionParser() {
        return new OptionParser() {
            {
                accepts(HELP_ARG, "display help text");

                accepts(CONFIGURATION_FILE,
                        "configuration file; CLI " + "parameters override configured parameters!")
                        .withRequiredArg().ofType(String.class)
                        .defaultsTo(DEFAULT_CONFIGURATION_FILE);

                accepts(SLEEP,
                        "number of minutes to sleep between cycles.\nThis parameter will be "
                                + "adjusted such that it is always greater than the scale.\nIf "
                                + "a value of 0 is given, the downloader exits "
                                + "after one dataset has been downloaded.").withRequiredArg()
                        .ofType(Integer.class).defaultsTo(DEFAULT_SLEEP);

                accepts(EmporiaAPIService.USERNAME, "username").withRequiredArg()
                        .ofType(String.class);
                accepts(EmporiaAPIService.PASSWORD, "password").withRequiredArg()
                        .ofType(String.class);

                accepts(INFLUX_URL, "InfluxDB server URL").withRequiredArg().ofType(String.class)
                        .defaultsTo(DEFAULT_INFLUX_URL);
                accepts(INFLUX_PORT, "InfluxDB server port").withRequiredArg()
                        .ofType(Integer.class).defaultsTo(DEFAULT_INFLUX_PORT);
                accepts(INFLUX_USER, "InfluxDB server username").withRequiredArg()
                        .ofType(String.class);
                accepts(INFLUX_PASS, "InfluxDB server password").withRequiredArg()
                        .ofType(String.class);
                accepts(INFLUX_DB, "InfluxDB database").withRequiredArg().ofType(String.class)
                        .defaultsTo(DEFAULT_INFLUX_DB);
                accepts(DISABLE_INFLUX, "disable the uploading to InfluxDB");

                accepts(LoggingConfigurator.RAW, "output raw JSON readings to STDOUT");

                accepts(EmporiaAPIService.SCALE, "scale of the data\nFor example, '--scale d' will "
                        + "download a datapoint per day.").withRequiredArg()
                        .withValuesConvertedBy(new ScaleConverter())
                        .defaultsTo(new Scale("s"));

                acceptsAll(asList(HISTORY, OFFSET),
                        "history to download if no prior data is available.\nFor "
                                + "example, '--history 3w' downloads the last 3 weeks of data"
                                + ".\nNote that the availability of data is subject to Emporia's"
                                + " data retention policies.").withRequiredArg()
                        .withValuesConvertedBy(new TemporalAmountConverter())
                        .defaultsTo(new DefaultTemporalAmount("3h"));

                accepts(LoggingConfigurator.LOGFILE, "log to this file").withRequiredArg()
                        .defaultsTo(DEFAULT_LOG_FILE);

                acceptsAll(asList("d", LoggingConfigurator.DEBUG), "enable debug messages.");
                acceptsAll(asList("q", LoggingConfigurator.QUIET),
                        "do not print any messages to the console except for errors.");
            }
        };
    }

    private static void printHelp(OptionParser parser) {
        try {
            parser.formatHelpWith(new BuiltinHelpFormatter(100, 2));
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    static Configuration getConfiguration(OptionSet optionSet) throws ConfigurationException {

        String confFileName = optionSet.valueOf(CONFIGURATION_FILE).toString();
        File confFile = new File(confFileName);

        PropertiesConfiguration config = new PropertiesConfiguration();
        if (confFile.exists() && confFile.canRead()) {
            config.load(confFile);
        }

        if (config.containsKey(HISTORY)) {
            config.setProperty(HISTORY,
                    new TemporalAmountConverter().convert(config.getString(HISTORY)));
        }

        if (config.containsKey(EmporiaAPIService.SCALE)) {
            config.setProperty(EmporiaAPIService.SCALE,
                    new ScaleConverter().convert(config.getString(EmporiaAPIService.SCALE)));
        }

        try {
            for (OptionSpec<?> optionSpec : optionSet.specs()) {
                String property = optionSpec.options().get(optionSpec.options().size() - 1);
                if (optionSet.valuesOf(optionSpec).size() == 0) {
                    config.setProperty(property, true);
                    continue;
                }

                for (Object value : optionSet.valuesOf(optionSpec)) {
                    config.setProperty(property, value);
                }
            }
        } catch (Exception e) {
            throw new ConfigurationException("Cannot parse options!", e);
        }

        // backwards compatibility, otherwise we could add to required args
        if (config.containsKey(LoggingConfigurator.RAW) && config
                .getProperty(LoggingConfigurator.RAW) instanceof Boolean && !config
                .getBoolean(LoggingConfigurator.RAW)) {
            config.clearProperty(LoggingConfigurator.RAW);
        } else if (config.containsKey(LoggingConfigurator.RAW) && config
                .getProperty(LoggingConfigurator.RAW) instanceof String && config
                .getString(LoggingConfigurator.RAW).equals("true")) {
            config.setProperty(LoggingConfigurator.RAW, true);
        }

        List<String> optionsToCheck = new ArrayList<>();
        optionsToCheck.addAll(REQUIRED_PARAMETERS);
        optionsToCheck.addAll(HAS_DEFAULT_VALUES);

        for (String option : optionsToCheck) {
            if (!config.containsKey(option) || config.getProperty(option) == null) {
                Object value = optionSet.valueOf(option);
                if (value == null) {
                    throw new ConfigurationException("Missing parameter " + option);
                }
                config.setProperty(option, value);
            }
        }

        if (config.getInt(SLEEP) > 0) {
            Scale scale = (Scale) config.getProperty(EmporiaAPIService.SCALE);
            int mins = Math.max(config.getInt(SLEEP), (int) scale.toInterval().toMinutes());
            config.setProperty(SLEEP, mins);
        }

        Instant history = Instant.now().minus((TemporalAmount) config.getProperty(HISTORY));
        config.setProperty(HISTORY, history);

        return config;
    }

    protected EmporiaDownloader(Configuration configuration) {
        this.configuration = configuration;
    }

    protected void run() {
        log.info("Starting run!");

        service = new EmporiaAPIService(configuration);

        if (service.isDownForMaintenance()) {
            log.warn("Service is down for maintenance. Exciting!");
            System.exit(0);
        }

        Customer customer = service.getCustomer();

        if (customer == null || customer.getDevices() == null || customer.getDevices().isEmpty()) {
            log.warn("Customer " + customer + " has no devices!");
            System.exit(0);
        }

        InfluxDBLoader influxDBLoader = null;
        if (!configuration.getBoolean(DISABLE_INFLUX, false)) {
            URI influxDbUri = null;
            try {
                influxDbUri = new URI((String) configuration.getString(INFLUX_URL));
                influxDbUri = new URI(influxDbUri.getScheme(), influxDbUri.getUserInfo(),
                        influxDbUri.getHost(), configuration.getInt(INFLUX_PORT),
                        influxDbUri.getPath(), influxDbUri.getQuery(), influxDbUri.getFragment());
                influxDBLoader = new InfluxDBLoader(influxDbUri.toURL(),
                        configuration.getString(INFLUX_USER), configuration.getString(INFLUX_PASS),
                        configuration.getString(INFLUX_DB));
            } catch (Exception e) {
                log.error("Cannot instantiate InfluxDBLoader", e);
            }

            InfluxDBLoader finalInfluxDBLoader = influxDBLoader;
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> Objects.requireNonNull(finalInfluxDBLoader).writeToDB()));

            for (Device device : customer.getDevices()) {
                loadChannelData(device, influxDBLoader);
                for (Device attachedDevice : device.getDevices()) {
                    loadChannelData(attachedDevice, influxDBLoader);
                }
            }
        }

        while (true) {
            for (Device device : customer.getDevices()) {
                processDevice(device, influxDBLoader);
                for (Device attachedDevice : device.getDevices()) {
                    processDevice(attachedDevice, influxDBLoader);
                }
            }

            if (influxDBLoader != null) {
                log.info("current write count: " + influxDBLoader.getWritesCount());
            }

            if (cannotSleep()) {
                break;
            }

            if (service.isDownForMaintenance()) {
                log.warn("Service is down for maintenance. Stand by!");
                if (cannotSleep()) {
                    break;
                }
            }
        }

        if (influxDBLoader != null) {
            influxDBLoader.writeToDB();
        }

        System.exit(0);
    }

    private boolean cannotSleep() {
        if (configuration.getInt(SLEEP) == 0) {
            return true;
        }

        try {
            Thread.sleep(1000 * 60 * configuration.getInt(SLEEP));
        } catch (InterruptedException e) {
            log.warn("Interrupt: " + e.getMessage());
            return true;
        }

        return false;
    }

    private void loadChannelData(Device device, InfluxDBLoader influxDBLoader) {
        if (influxDBLoader == null) {
            return;
        }
        for (Channel channel : device.getChannels()) {
            Readings readings = influxDBLoader.load(channel);
            if (readings.getEnd() != null) {
                log.debug("last reading for " + channel + ": " + readings);
                lastDataPoint.put(channel, readings.getEnd());
            } else {
                log.warn("no readings loaded for " + channel);
            }
        }
    }

    protected void processDevice(Device device, InfluxDBLoader influxDBLoader) {
        Instant start;
        Scale scale = (Scale) configuration.getProperty(EmporiaAPIService.SCALE);
        for (Channel channel : device.getChannels()) {
            if (lastDataPoint.containsKey(channel)) {
                start = lastDataPoint.get(channel);
            } else {
                start = (Instant) configuration.getProperty(HISTORY);
                lastDataPoint.put(channel, start);
            }

            Instant now = Instant.now();
            long secs = scale.getAmount().get(ChronoUnit.SECONDS);
            long epSecs = now.minus(start.toEpochMilli(), ChronoUnit.MILLIS).getEpochSecond();
            long buckets = Math.min(epSecs/secs, 2000);

            Instant end = start.plus(buckets, scale.getChronoUnit());

            do {
                log.debug("channel: " + channel + " " + start + " - " + end);
                Readings readings = service.getReadings(channel, start, end);

                if (readings == null || readings.getUsageList().size() == 0 || readings.getStart()
                        .equals(readings.getEnd())) {
                    log.warn("Received empty/null readings. Skipping channel for now!");
                    log.warn("Readings: " + readings);
                    break;
                }

                log.debug("Readings: " + readings);

                if (influxDBLoader != null) {
                    influxDBLoader.save(readings);
                }

                if (readings.getEnd() == null || readings.getStart().equals(readings.getEnd())) {
                    break;
                }

                start = readings.getEnd();
                lastDataPoint.put(channel, start);
                end = start.plus(start.toEpochMilli() - readings.getStart().toEpochMilli(),
                        ChronoUnit.MILLIS);
            } while (end.isBefore(now));
        }
    }
}
