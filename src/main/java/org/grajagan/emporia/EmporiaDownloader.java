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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.grajagan.aws.AuthTokenClientRequestFilter;
import org.grajagan.aws.CognitoAuthenticationManager;
import org.grajagan.emporia.influxdb.InfluxDBLoader;
import org.grajagan.emporia.model.Channel;
import org.grajagan.emporia.model.Customer;
import org.grajagan.emporia.model.Device;
import org.grajagan.emporia.model.Maintenance;
import org.grajagan.emporia.model.Readings;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
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

    private static final String REGION = "region";
    private static final String CLIENTAPP_ID = "clientapp-id";
    private static final String POOL_ID = "pool-id";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String INFLUX_URL = "influx-url";
    private static final String INFLUX_PORT = "influx-port";
    private static final String INFLUX_USER = "influx-user";
    private static final String INFLUX_PASS = "influx-password";
    private static final String INFLUX_DB = "influx-db";
    private static final String DISABLE_INFLUX = "disable-influx";

    private static final String RAW = "raw";
    private static final String OFFSET = "offset";

    private static final String SLEEP = "sleep";

    private static final String DEFAULT_CONFIGURATION_FILE =
            Paths.get("config.properties").toAbsolutePath().toString();

    private static final String DEFAULT_LOCAL_HOST = "localhost";
    private static final String DEFAULT_INFLUX_URL = "http://" + DEFAULT_LOCAL_HOST;
    private static final int DEFAULT_INFLUX_PORT = 8086;
    private static final String DEFAULT_INFLUX_DB = "electricity";

    private static final String DEFAULT_LOG_FILE =
            Paths.get("application.log").toAbsolutePath().toString();

    private static final Integer DEFAULT_SLEEP = 5;

    private static final TemporalAmount DEFAULT_OFFSET = new CommandLineTemporalUnit();

    private static final List<String> REQUIRED_PARAMETERS = new ArrayList<>();

    static {
        REQUIRED_PARAMETERS.addAll(Arrays
                .asList(REGION, CLIENTAPP_ID, POOL_ID, USERNAME, PASSWORD, SLEEP, OFFSET));
    }

    private static final String API_URL = "https://api.emporiaenergy.com";

    private Configuration configuration;

    private Map<Channel, Instant> lastDataPoint = new HashMap<>();

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser() {
            {
                accepts(HELP_ARG, "display help text");

                accepts(CONFIGURATION_FILE,
                        "configuration file; CLI "
                                + "parameters override configured parameters!").withRequiredArg()
                        .ofType(String.class).defaultsTo(DEFAULT_CONFIGURATION_FILE);

                accepts(SLEEP, "number of minutes to sleep between cycles")
                        .withRequiredArg().ofType(Integer.class).defaultsTo(DEFAULT_SLEEP);

                accepts(REGION, "AWS region").withRequiredArg().ofType(String.class);
                accepts(CLIENTAPP_ID, "AWS client ID").withRequiredArg().ofType(String.class);
                accepts(POOL_ID, "AWS user pool ID").withRequiredArg().ofType(String.class);

                accepts(USERNAME, "username").withRequiredArg().ofType(String.class);
                accepts(PASSWORD, "password").withRequiredArg().ofType(String.class);

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

                accepts(RAW, "output raw JSON readings to STDOUT");

                accepts(OFFSET,
                        "time offset if no prior data is available (number plus time unit; one "
                                + "of 's', 'm', or 'h')").withRequiredArg()
                        .withValuesConvertedBy(new OffsetConverter()).defaultsTo(DEFAULT_OFFSET);

                accepts(LoggingConfigurator.LOGFILE, "log to this file")
                        .withOptionalArg().defaultsTo(DEFAULT_LOG_FILE);

                acceptsAll(asList("d", LoggingConfigurator.DEBUG), "enable debug messages.");
                acceptsAll(asList("q", LoggingConfigurator.QUIET),
                        "do not print any messages to the console except for errors.");
            }
        };

        OptionSet options = parser.parse(argv);
        if (options.has(HELP_ARG)) {
            printHelp(parser);
            System.exit(0);
        }

        if (!options.has(DISABLE_INFLUX)) {
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

    private static void printHelp(OptionParser parser) {
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected static Configuration getConfiguration(OptionSet optionSet)
            throws ConfigurationException {
        Configuration config =
                new PropertiesConfiguration(optionSet.valueOf(CONFIGURATION_FILE).toString());

        try {
            for (OptionSpec<?> optionSpec : optionSet.specs()) {
                String property = optionSpec.options().get(optionSpec.options().size() - 1);
                if (optionSet.valuesOf(optionSpec).size() == 0) {
                    config.addProperty(property, true);
                    continue;
                }

                for (Object value : optionSet.valuesOf(optionSpec)) {
                    config.addProperty(property, value);
                }
            }
        } catch (Exception e) {
            throw new ConfigurationException("Cannot parse options!", e);
        }

        for (String option : REQUIRED_PARAMETERS) {
            if (!config.containsKey(option) || config.getProperty(option) == null) {
                Object value = optionSet.valueOf(option);
                if (value == null) {
                    throw new ConfigurationException("Missing parameter " + option);
                }
                config.addProperty(option, value);
            }
        }

        return config;
    }

    protected EmporiaDownloader(Configuration configuration) {
        this.configuration = configuration;
    }

    protected void run() {
        log.info("Starting run!");

        CognitoAuthenticationManager authenticationManager =
                CognitoAuthenticationManager.builder().username(configuration.getString(USERNAME))
                        .password(configuration.getString(PASSWORD))
                        .poolId(configuration.getString(POOL_ID))
                        .region(configuration.getString(REGION))
                        .clientId(configuration.getString(CLIENTAPP_ID)).build();

        ClientConfig clientConfig = new ClientConfig();

        clientConfig.register(AuthTokenClientRequestFilter.class);
        clientConfig.property(AuthTokenClientRequestFilter.AUTHENTICATION_MANAGER,
                authenticationManager);

        Client sslClient = initClient(clientConfig, true);

        Customer customer = getCustomer(sslClient, configuration.getString(USERNAME));

        if (customer.getDevices() == null || customer.getDevices().isEmpty()) {
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

        Client httpClient = initClient(new ClientConfig(), false);

        while (true) {
            try {
                Maintenance maintenance =
                        (Maintenance) getObject(httpClient, Maintenance.TARGET_URL,
                                Maintenance.class);

                if (maintenance.getMsg() != null && maintenance.getMsg().equals("down")) {
                    log.warn("API endpoint is down for maintenance; pausing.");
                    if (!sleep()) {
                        break;
                    } else {
                        continue;
                    }
                }
            } catch (MessageBodyProviderNotFoundException e) {
                // not sure why they can't send back a decent response when all is working
            }

            for (Device device : customer.getDevices()) {
                processDevice(sslClient, device, influxDBLoader);
                for (Device attachedDevice : device.getDevices()) {
                    processDevice(sslClient, attachedDevice, influxDBLoader);
                }
            }

            if (influxDBLoader != null) {
                log.info("current write count: " + influxDBLoader.getWritesCount());
            }

            if (!sleep()) {
                break;
            }
        }

        if (influxDBLoader != null) {
            influxDBLoader.writeToDB();
        }
    }

    private boolean sleep() {
        try {
            Thread.sleep(1000 * 60 * configuration.getInt(SLEEP));
        } catch (InterruptedException e) {
            log.warn("Interrupt: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void loadChannelData(Device device, InfluxDBLoader influxDBLoader) {
        if (influxDBLoader == null) {
            return;
        }
        for (Channel channel : device.getChannels()) {
            Readings readings = influxDBLoader.load(channel);
            log.debug("last reading for " + channel + ": " + readings);
            lastDataPoint.put(channel, readings.getEnd());
        }
    }

    protected void processDevice(Client client, Device device, InfluxDBLoader influxDBLoader) {
        Instant start;
        for (Channel channel : device.getChannels()) {
            if (lastDataPoint.containsKey(channel)) {
                start = lastDataPoint.get(channel);
            } else {
                start = Instant.now().minus((TemporalAmount) configuration.getProperty(OFFSET));
                lastDataPoint.put(channel, start);
            }

            Instant now = Instant.now();
            Instant end = now.minus(1, ChronoUnit.MILLIS);

            ObjectMapper mapper = new ObjectMapper();

            while (end.isBefore(now)) {
                log.debug("channel: " + channel + " " + start + " - " + end);
                Readings readings = getReadings(client, channel, start, end);
                if (influxDBLoader != null) {
                    influxDBLoader.save(readings);
                }

                if (configuration.getBoolean(RAW, false)) {
                    try {
                        System.out.println(mapper.writeValueAsString(readings));
                    } catch (JsonProcessingException e) {
                        log.error("Cannot convert readings to JSON", e);
                    }
                }

                log.debug(readings);
                if (readings.getEnd() != null) {
                    start = readings.getEnd();
                    lastDataPoint.put(channel, start);
                    end = start.plus(start.toEpochMilli() - readings.getStart().toEpochMilli(),
                            ChronoUnit.MILLIS);
                } else {
                    break;
                }
            }
        }
    }

    protected Customer getCustomer(Client client, String email) {
        String url = API_URL + "/customers?email=" + email;
        Customer customer = (Customer) getObject(client, url, Customer.class);

        url = API_URL + "/customers/" + customer.getCustomerGid()
                + "/devices?detailed=true&hierarchy=true";

        customer.setDevices(((Customer) getObject(client, url, Customer.class)).getDevices());
        return customer;
    }

    protected Readings getReadings(Client client, Channel channel, Instant start, Instant end) {
        String url = API_URL + "/usage/time?start=%s&end=%s&type=%s&deviceGid=%d&scale"
                + "=%s&unit=%s&channels=%s";
        url = String.format(url, start, end, Readings.DEFAULT_TYPE, channel.getDeviceGid(),
                Readings.DEFAULT_SCALE, Readings.DEFAULT_UNIT, channel.getChannelNum());
        Readings readings = (Readings) getObject(client, url, Readings.class);
        readings.setChannel(channel);
        return readings;
    }

    protected Object getObject(Client client, String url, Class<?> clazz) {
        WebTarget target = client.target(url);
        Invocation.Builder builder = target.request();
        Response response = builder.get();
        return response.readEntity(clazz);
    }

    protected Client initClient(ClientConfig config, boolean doSSL) {
        ClientBuilder builder =
                ClientBuilder.newBuilder().withConfig(config).register(JacksonConfig.class);

        if (doSSL) {
            SSLContext ctx = null;
            try {
                ctx = SSLContext.getInstance("SSL");
                ctx.init(null, certs, new SecureRandom());
            } catch (Exception e) {
                log.error("Cannot init context!", e);
                System.exit(1);
            }

            builder.hostnameVerifier(new TrustAllHostNameVerifier()).sslContext(ctx);
        }

        return builder.build();
    }

    TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
    } };

    public static class TrustAllHostNameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
